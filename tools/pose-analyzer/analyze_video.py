#!/usr/bin/env python3
"""Analyze workout videos with MediaPipe Pose and export rep segments as JSON."""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import cv2
import mediapipe as mp
import numpy as np
from scipy.signal import find_peaks, savgol_filter

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_OUTPUT_DIR = SCRIPT_DIR / "output"

LANDMARK_ALIASES = {
    "LEFT_KNEE": mp.solutions.pose.PoseLandmark.LEFT_KNEE,
    "RIGHT_KNEE": mp.solutions.pose.PoseLandmark.RIGHT_KNEE,
    "LEFT_HIP": mp.solutions.pose.PoseLandmark.LEFT_HIP,
    "RIGHT_HIP": mp.solutions.pose.PoseLandmark.RIGHT_HIP,
    "LEFT_SHOULDER": mp.solutions.pose.PoseLandmark.LEFT_SHOULDER,
    "RIGHT_SHOULDER": mp.solutions.pose.PoseLandmark.RIGHT_SHOULDER,
    "LEFT_WRIST": mp.solutions.pose.PoseLandmark.LEFT_WRIST,
    "RIGHT_WRIST": mp.solutions.pose.PoseLandmark.RIGHT_WRIST,
    "LEFT_ANKLE": mp.solutions.pose.PoseLandmark.LEFT_ANKLE,
    "RIGHT_ANKLE": mp.solutions.pose.PoseLandmark.RIGHT_ANKLE,
    "NOSE": mp.solutions.pose.PoseLandmark.NOSE,
}


@dataclass
class FrameSample:
    timestamp_ms: int
    y: float
    pose_landmarks: object | None


def parse_landmark(name: str) -> mp.solutions.pose.PoseLandmark:
    key = name.strip().upper()
    if key not in LANDMARK_ALIASES:
        valid = ", ".join(sorted(LANDMARK_ALIASES))
        raise ValueError(f"Unknown landmark '{name}'. Valid options: {valid}")
    return LANDMARK_ALIASES[key]


def smooth_series(values: np.ndarray, window: int) -> np.ndarray:
    if len(values) < 3:
        return values.copy()

    window = min(window, len(values) if len(values) % 2 == 1 else len(values) - 1)
    if window < 3:
        return values.copy()
    if window % 2 == 0:
        window -= 1

    return savgol_filter(values, window_length=window, polyorder=2)


def detect_rep_segments(
    timestamps_ms: np.ndarray,
    y_values: np.ndarray,
    min_rep_duration_ms: int,
    rep_boundary: str,
) -> list[dict[str, int]]:
    if len(timestamps_ms) < 3:
        return []

    avg_frame_ms = max(1, int(np.median(np.diff(timestamps_ms))))
    min_distance = max(1, int(min_rep_duration_ms / avg_frame_ms))

    y_smooth = smooth_series(y_values, window=max(5, min_distance | 1))

    peaks, _ = find_peaks(y_smooth, distance=min_distance)
    valleys, _ = find_peaks(-y_smooth, distance=min_distance)

    if rep_boundary == "peak":
        boundary_indices = peaks
    else:
        boundary_indices = valleys

    if len(boundary_indices) < 2:
        # Fallback: try the other extrema type if the preferred one is sparse.
        fallback = valleys if rep_boundary == "peak" else peaks
        if len(fallback) >= 2:
            boundary_indices = fallback

    reps: list[dict[str, int]] = []
    for start_idx, end_idx in zip(boundary_indices[:-1], boundary_indices[1:]):
        start_ms = int(timestamps_ms[start_idx])
        end_ms = int(timestamps_ms[end_idx])
        if end_ms - start_ms >= min_rep_duration_ms:
            reps.append({"start": start_ms, "end": end_ms})

    return reps


def extract_landmark_series(
    video_path: Path,
    landmark: mp.solutions.pose.PoseLandmark,
    model_complexity: int = 1,
) -> tuple[list[FrameSample], float, tuple[int, int]]:
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    samples: list[FrameSample] = []
    last_valid_y: float | None = None
    frame_index = 0

    mp_pose = mp.solutions.pose
    with mp_pose.Pose(
        static_image_mode=False,
        model_complexity=model_complexity,
        enable_segmentation=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    ) as pose:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            timestamp_ms = int(round((frame_index / fps) * 1000))
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result = pose.process(rgb)

            y_value: float | None = None
            if result.pose_landmarks:
                point = result.pose_landmarks.landmark[landmark.value]
                if point.visibility >= 0.5:
                    y_value = float(point.y)
                    last_valid_y = y_value
                elif last_valid_y is not None:
                    y_value = last_valid_y

            if y_value is None:
                frame_index += 1
                continue

            samples.append(
                FrameSample(
                    timestamp_ms=timestamp_ms,
                    y=y_value,
                    pose_landmarks=result.pose_landmarks,
                )
            )

            frame_index += 1

    cap.release()
    return samples, fps, (width, height)


def render_debug_video(
    video_path: Path,
    output_path: Path,
    landmark: mp.solutions.pose.PoseLandmark,
    reps: Iterable[dict[str, int]],
    model_complexity: int = 1,
) -> None:
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video for debug render: {video_path}")

    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    writer = cv2.VideoWriter(str(output_path), fourcc, fps, (width, height))

    rep_starts = {rep["start"] for rep in reps}
    rep_ends = {rep["end"] for rep in reps}
    active_rep = 0

    mp_pose = mp.solutions.pose
    mp_drawing = mp.solutions.drawing_utils
    mp_styles = mp.solutions.drawing_styles

    frame_index = 0
    with mp_pose.Pose(
        static_image_mode=False,
        model_complexity=model_complexity,
        enable_segmentation=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    ) as pose:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            timestamp_ms = int(round((frame_index / fps) * 1000))
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result = pose.process(rgb)

            if result.pose_landmarks:
                mp_drawing.draw_landmarks(
                    frame,
                    result.pose_landmarks,
                    mp_pose.POSE_CONNECTIONS,
                    landmark_drawing_spec=mp_styles.get_default_pose_landmarks_style(),
                )

                point = result.pose_landmarks.landmark[landmark.value]
                px = int(point.x * width)
                py = int(point.y * height)
                cv2.circle(frame, (px, py), 10, (0, 165, 255), -1)
                cv2.putText(
                    frame,
                    landmark.name,
                    (px + 12, py - 12),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (0, 165, 255),
                    2,
                    cv2.LINE_AA,
                )

            for rep_idx, rep in enumerate(reps, start=1):
                if rep["start"] <= timestamp_ms <= rep["end"]:
                    active_rep = rep_idx
                    cv2.rectangle(frame, (0, 0), (width, height), (0, 255, 0), 4)
                    break

            if timestamp_ms in rep_starts:
                cv2.putText(
                    frame,
                    "REP START",
                    (20, 50),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1.0,
                    (0, 255, 0),
                    3,
                    cv2.LINE_AA,
                )

            if timestamp_ms in rep_ends:
                cv2.putText(
                    frame,
                    "REP END",
                    (20, 90),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1.0,
                    (0, 0, 255),
                    3,
                    cv2.LINE_AA,
                )

            cv2.putText(
                frame,
                f"Rep: {active_rep if active_rep else '-'}",
                (20, height - 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                (255, 255, 255),
                2,
                cv2.LINE_AA,
            )

            writer.write(frame)
            frame_index += 1

    cap.release()
    writer.release()


def build_output_payload(video_path: Path, reps: list[dict[str, int]]) -> dict:
    return {
        "videoFile": video_path.name,
        "reps": reps,
    }


def analyze_video(
    input_path: Path,
    output_dir: Path,
    landmark_name: str,
    min_rep_duration_ms: int,
    rep_boundary: str,
    debug: bool,
) -> Path:
    landmark = parse_landmark(landmark_name)
    samples, fps, _ = extract_landmark_series(input_path, landmark)

    if len(samples) < 3:
        raise RuntimeError(
            "Not enough pose detections in video. "
            "Try another landmark, improve lighting, or ensure the full body is visible."
        )

    timestamps = np.array([sample.timestamp_ms for sample in samples], dtype=np.int64)
    y_values = np.array([sample.y for sample in samples], dtype=np.float64)

    reps = detect_rep_segments(
        timestamps_ms=timestamps,
        y_values=y_values,
        min_rep_duration_ms=min_rep_duration_ms,
        rep_boundary=rep_boundary,
    )

    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / f"{input_path.stem}.json"
    payload = build_output_payload(input_path, reps)

    with json_path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")

    print(f"Processed frames: {len(samples)} @ {fps:.2f} fps")
    print(f"Landmark: {landmark.name}")
    print(f"Detected reps: {len(reps)}")
    print(f"JSON written to: {json_path}")

    if debug:
        debug_path = output_dir / f"{input_path.stem}_debug.mp4"
        render_debug_video(
            video_path=input_path,
            output_path=debug_path,
            landmark=landmark,
            reps=reps,
        )
        print(f"Debug video written to: {debug_path}")

    return json_path


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Detect workout repetitions in a video using MediaPipe Pose and "
            "export Clip-It Game Mode JSON."
        )
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Path to the input video file.",
    )
    parser.add_argument(
        "--landmark",
        default="LEFT_KNEE",
        help=(
            "Pose landmark to track (default: LEFT_KNEE). "
            "Examples: RIGHT_SHOULDER, LEFT_HIP, RIGHT_WRIST."
        ),
    )
    parser.add_argument(
        "--min-rep-duration-ms",
        type=int,
        default=500,
        help="Minimum duration between rep boundaries in milliseconds (default: 500).",
    )
    parser.add_argument(
        "--rep-boundary",
        choices=["valley", "peak"],
        default="valley",
        help=(
            "Use consecutive valleys (low Y / top position) or peaks (high Y / bottom) "
            "as rep boundaries. Default: valley."
        ),
    )
    parser.add_argument(
        "--output-dir",
        default=str(DEFAULT_OUTPUT_DIR),
        help=f"Directory for JSON/debug output (default: {DEFAULT_OUTPUT_DIR}).",
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Render a debug video with skeleton and rep markers.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        print(f"Input video not found: {input_path}", file=sys.stderr)
        return 1

    if args.min_rep_duration_ms < 100:
        print("--min-rep-duration-ms must be at least 100.", file=sys.stderr)
        return 1

    try:
        analyze_video(
            input_path=input_path,
            output_dir=Path(args.output_dir).expanduser().resolve(),
            landmark_name=args.landmark,
            min_rep_duration_ms=args.min_rep_duration_ms,
            rep_boundary=args.rep_boundary,
            debug=args.debug,
        )
    except Exception as exc:  # noqa: BLE001 - CLI entrypoint
        print(f"Error: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
