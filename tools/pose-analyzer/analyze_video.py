#!/usr/bin/env python3
"""Analyze workout videos with MediaPipe Pose and export rep segments as JSON."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import cv2
import mediapipe as mp
import numpy as np
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode
from scipy.signal import find_peaks, savgol_filter

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_OUTPUT_DIR = SCRIPT_DIR / "output"
MODELS_DIR = SCRIPT_DIR / "models"
MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/"
    "pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
)

# BlazePose landmark indices
LANDMARK_INDEX = {
    "NOSE": 0,
    "LEFT_SHOULDER": 11,
    "RIGHT_SHOULDER": 12,
    "LEFT_ELBOW": 13,
    "RIGHT_ELBOW": 14,
    "LEFT_WRIST": 15,
    "RIGHT_WRIST": 16,
    "LEFT_HIP": 23,
    "RIGHT_HIP": 24,
    "LEFT_KNEE": 25,
    "RIGHT_KNEE": 26,
    "LEFT_ANKLE": 27,
    "RIGHT_ANKLE": 28,
    "MID_SHOULDER": -1,
    "MID_HIP": -2,
}

SYNTHETIC_LANDMARKS = {
    "MID_SHOULDER": ("LEFT_SHOULDER", "RIGHT_SHOULDER"),
    "MID_HIP": ("LEFT_HIP", "RIGHT_HIP"),
}

MOVEMENT_CANDIDATES = {
    "core": (
        "NOSE",
        "MID_SHOULDER",
        "MID_HIP",
        "LEFT_SHOULDER",
        "RIGHT_SHOULDER",
        "LEFT_HIP",
        "RIGHT_HIP",
    ),
    "upper": (
        "LEFT_WRIST",
        "RIGHT_WRIST",
        "LEFT_ELBOW",
        "RIGHT_ELBOW",
        "LEFT_SHOULDER",
        "RIGHT_SHOULDER",
    ),
    "lower": (
        "LEFT_KNEE",
        "RIGHT_KNEE",
        "LEFT_ANKLE",
        "RIGHT_ANKLE",
        "LEFT_HIP",
        "RIGHT_HIP",
    ),
    "full": (
        "NOSE",
        "MID_SHOULDER",
        "MID_HIP",
        "LEFT_WRIST",
        "RIGHT_WRIST",
        "LEFT_ELBOW",
        "RIGHT_ELBOW",
        "LEFT_SHOULDER",
        "RIGHT_SHOULDER",
        "LEFT_HIP",
        "RIGHT_HIP",
        "LEFT_KNEE",
        "RIGHT_KNEE",
    ),
}

AUTO_CANDIDATES = MOVEMENT_CANDIDATES["full"]
MAX_PEOPLE = 5
FACE_INDICES = (0, 2, 5, 7, 8)  # nose, eyes, ears

POSE_CONNECTIONS = (
    (11, 12),
    (11, 13),
    (13, 15),
    (12, 14),
    (14, 16),
    (11, 23),
    (12, 24),
    (23, 24),
    (23, 25),
    (25, 27),
    (24, 26),
    (26, 28),
)


@dataclass
class FrameSample:
    timestamp_ms: int
    y: float
    landmarks: list | None


@dataclass
class PersonTracker:
    last_center: tuple[float, float] | None = None


def ensure_pose_model() -> Path:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    model_path = MODELS_DIR / "pose_landmarker_lite.task"
    if model_path.exists():
        return model_path

    print(f"Downloading pose model to {model_path} ...")
    urllib.request.urlretrieve(MODEL_URL, model_path)
    return model_path


def create_pose_landmarker(max_people: int = MAX_PEOPLE) -> PoseLandmarker:
    options = PoseLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(ensure_pose_model())),
        running_mode=RunningMode.VIDEO,
        num_poses=max(1, max_people),
        min_pose_detection_confidence=0.5,
        min_pose_presence_confidence=0.5,
        min_tracking_confidence=0.5,
    )
    return PoseLandmarker.create_from_options(options)


def face_visibility_score(landmarks: list) -> float:
    nose = landmarks[0]
    if nose.visibility < 0.35:
        return 0.0

    face_points = [landmarks[index] for index in FACE_INDICES]
    mean_visibility = float(np.mean([point.visibility for point in face_points]))
    if nose.visibility < 0.5:
        mean_visibility *= 0.5
    return mean_visibility


def pose_body_size(landmarks: list) -> float:
    visible = [point for point in landmarks if point.visibility >= 0.5]
    if len(visible) < 4:
        return 0.0

    xs = [point.x for point in visible]
    ys = [point.y for point in visible]
    return float((max(xs) - min(xs)) * (max(ys) - min(ys)))


def pose_center(landmarks: list) -> tuple[float, float]:
    if landmarks[0].visibility >= 0.5:
        return float(landmarks[0].x), float(landmarks[0].y)

    points = []
    for index in (11, 12, 23, 24):
        point = landmarks[index]
        if point.visibility >= 0.5:
            points.append(point)

    if not points:
        return float(landmarks[0].x), float(landmarks[0].y)

    return (
        float(np.mean([point.x for point in points])),
        float(np.mean([point.y for point in points])),
    )


def pick_primary_pose(
    poses: list[list],
    tracker: PersonTracker,
) -> tuple[list | None, PersonTracker]:
    if not poses:
        return None, tracker

    best_pose: list | None = None
    best_score = -1.0
    best_center: tuple[float, float] | None = None

    for pose in poses:
        face_score = face_visibility_score(pose)
        if face_score <= 0.0:
            continue

        body_size = pose_body_size(pose)
        center = pose_center(pose)
        score = face_score * 3.0 + body_size

        if tracker.last_center is not None:
            distance = np.hypot(
                center[0] - tracker.last_center[0],
                center[1] - tracker.last_center[1],
            )
            score += max(0.0, 1.0 - (distance / 0.25))

        if score > best_score:
            best_score = score
            best_pose = pose
            best_center = center

    if best_pose is None:
        # Fallback: largest body if no visible face in frame.
        for pose in poses:
            body_size = pose_body_size(pose)
            if body_size > best_score:
                best_score = body_size
                best_pose = pose
                best_center = pose_center(pose)

    if best_pose is None:
        return None, tracker

    return best_pose, PersonTracker(last_center=best_center)


def parse_landmark(name: str) -> str:
    key = name.strip().upper()
    if key == "AUTO":
        return "AUTO"
    if key in SYNTHETIC_LANDMARKS or key in LANDMARK_INDEX:
        return key
    valid = ", ".join(["AUTO", *sorted({*LANDMARK_INDEX, *SYNTHETIC_LANDMARKS})])
    raise ValueError(f"Unknown landmark '{name}'. Valid options: {valid}")


def landmark_y(landmarks: list, landmark_name: str) -> tuple[float, float] | None:
    if landmark_name in SYNTHETIC_LANDMARKS:
        left_name, right_name = SYNTHETIC_LANDMARKS[landmark_name]
        left = landmark_y(landmarks, left_name)
        right = landmark_y(landmarks, right_name)
        if left is None or right is None:
            return None
        y_value = (left[0] + right[0]) / 2.0
        visibility = (left[1] + right[1]) / 2.0
        return y_value, visibility

    index = LANDMARK_INDEX[landmark_name]
    point = landmarks[index]
    if point.visibility < 0.5:
        return None
    return float(point.y), float(point.visibility)


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

    boundary_indices = peaks if rep_boundary == "peak" else valleys

    if len(boundary_indices) < 2:
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


def process_video_frames(
    video_path: Path,
    landmark_name: str,
    frame_step: int = 1,
) -> tuple[list[FrameSample], float, tuple[int, int], list[list]]:
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    samples: list[FrameSample] = []
    raw_landmarks: list[list] = []
    last_valid_y: float | None = None
    frame_index = 0
    tracker = PersonTracker()

    landmarker = create_pose_landmarker()
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            if frame_index % frame_step != 0:
                frame_index += 1
                continue

            timestamp_ms = int(round((frame_index / fps) * 1000))
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            result = landmarker.detect_for_video(mp_image, timestamp_ms)

            y_value: float | None = None
            frame_landmarks: list | None = None

            if result.pose_landmarks:
                frame_landmarks, tracker = pick_primary_pose(result.pose_landmarks, tracker)
                if frame_landmarks is not None:
                    measured = landmark_y(frame_landmarks, landmark_name)
                    if measured is not None:
                        y_value, _ = measured
                        last_valid_y = y_value
                    elif last_valid_y is not None:
                        y_value = last_valid_y

            if y_value is not None:
                samples.append(
                    FrameSample(
                        timestamp_ms=timestamp_ms,
                        y=y_value,
                        landmarks=frame_landmarks,
                    )
                )
                raw_landmarks.append(frame_landmarks)

            frame_index += 1
    finally:
        landmarker.close()

    cap.release()
    return samples, fps, (width, height), raw_landmarks


def auto_select_landmark(video_path: Path, movement: str) -> str:
    movement_key = movement.strip().lower()
    candidates = MOVEMENT_CANDIDATES.get(movement_key, MOVEMENT_CANDIDATES["full"])
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    frame_step = max(1, int(fps // 6))  # ~6 samples per second

    candidate_values: dict[str, list[float]] = {name: [] for name in candidates}
    candidate_visibility: dict[str, list[float]] = {name: [] for name in candidates}
    frame_index = 0
    tracker = PersonTracker()

    landmarker = create_pose_landmarker()
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            if frame_index % frame_step != 0:
                frame_index += 1
                continue

            timestamp_ms = int(round((frame_index / fps) * 1000))
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            result = landmarker.detect_for_video(mp_image, timestamp_ms)

            if result.pose_landmarks:
                landmarks, tracker = pick_primary_pose(result.pose_landmarks, tracker)
                if landmarks is None:
                    frame_index += 1
                    continue
                for name in candidates:
                    measured = landmark_y(landmarks, name)
                    if measured is not None:
                        y_value, visibility = measured
                        candidate_values[name].append(y_value)
                        candidate_visibility[name].append(visibility)

            frame_index += 1
    finally:
        landmarker.close()

    cap.release()

    best_name = candidates[0]
    best_score = -1.0

    for name in candidates:
        values = candidate_values[name]
        if len(values) < 5:
            continue

        arr = np.array(values, dtype=np.float64)
        value_range = float(np.percentile(arr, 95) - np.percentile(arr, 5))
        mean_visibility = float(np.mean(candidate_visibility[name]))
        score = value_range * mean_visibility
        if score > best_score:
            best_score = score
            best_name = name

    print(f"Auto-selected landmark: {best_name} for movement '{movement_key}' (score={best_score:.4f})")
    print("Person tracking: auto face/body selection across up to", MAX_PEOPLE, "people")
    return best_name


def draw_pose_overlay(
    frame: np.ndarray,
    landmarks: list,
    tracked_landmark: str,
    *,
    highlight: bool = True,
) -> None:
    height, width = frame.shape[:2]
    line_color = (0, 255, 0) if highlight else (120, 120, 120)
    line_thickness = 2 if highlight else 1

    for start_idx, end_idx in POSE_CONNECTIONS:
        start = landmarks[start_idx]
        end = landmarks[end_idx]
        if start.visibility < 0.5 or end.visibility < 0.5:
            continue
        x1, y1 = int(start.x * width), int(start.y * height)
        x2, y2 = int(end.x * width), int(end.y * height)
        cv2.line(frame, (x1, y1), (x2, y2), line_color, line_thickness)

    if not highlight:
        return

    nose = landmarks[0]
    if nose.visibility >= 0.5:
        nx = int(nose.x * width)
        ny = int(nose.y * height)
        cv2.circle(frame, (nx, ny), 8, (255, 0, 255), -1)
        cv2.putText(
            frame,
            "FACE",
            (nx + 10, ny - 10),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.5,
            (255, 0, 255),
            2,
            cv2.LINE_AA,
        )

    tracked = landmarks[LANDMARK_INDEX[tracked_landmark]] if tracked_landmark in LANDMARK_INDEX else None
    if tracked_landmark in SYNTHETIC_LANDMARKS:
        left_name, right_name = SYNTHETIC_LANDMARKS[tracked_landmark]
        left = landmarks[LANDMARK_INDEX[left_name]]
        right = landmarks[LANDMARK_INDEX[right_name]]
        px = int(((left.x + right.x) / 2.0) * width)
        py = int(((left.y + right.y) / 2.0) * height)
    elif tracked is not None:
        px = int(tracked.x * width)
        py = int(tracked.y * height)
    else:
        return
    cv2.circle(frame, (px, py), 10, (0, 165, 255), -1)
    cv2.putText(
        frame,
        tracked_landmark,
        (px + 12, py - 12),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.6,
        (0, 165, 255),
        2,
        cv2.LINE_AA,
    )


def render_debug_video(
    video_path: Path,
    output_path: Path,
    landmark_name: str,
    reps: Iterable[dict[str, int]],
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
    frame_index = 0
    tracker = PersonTracker()

    landmarker = create_pose_landmarker()
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            timestamp_ms = int(round((frame_index / fps) * 1000))
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            result = landmarker.detect_for_video(mp_image, timestamp_ms)

            if result.pose_landmarks:
                tracked_pose, tracker = pick_primary_pose(result.pose_landmarks, tracker)
                for pose in result.pose_landmarks:
                    is_tracked = tracked_pose is pose
                    draw_pose_overlay(
                        frame,
                        pose,
                        landmark_name,
                        highlight=is_tracked,
                    )

                if tracked_pose is not None:
                    cv2.putText(
                        frame,
                        "TRACKED PERSON",
                        (20, height - 70),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.8,
                        (0, 255, 0),
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
    finally:
        landmarker.close()

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
    movement: str,
    min_rep_duration_ms: int,
    rep_boundary: str,
    debug: bool,
) -> Path:
    requested = parse_landmark(landmark_name)
    selected_landmark = (
        auto_select_landmark(input_path, movement) if requested == "AUTO" else requested
    )

    samples, fps, _, _ = process_video_frames(input_path, selected_landmark)

    if len(samples) < 3:
        raise RuntimeError(
            "Not enough pose detections in video. "
            "Try another landmark, improve lighting, or ensure the body is visible."
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
    print(f"Landmark: {selected_landmark}")
    print(f"Detected reps: {len(reps)}")
    print(f"JSON written to: {json_path}")

    if debug:
        debug_path = output_dir / f"{input_path.stem}_debug.mp4"
        render_debug_video(
            video_path=input_path,
            output_path=debug_path,
            landmark_name=selected_landmark,
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
    parser.add_argument("--input", required=True, help="Path to the input video file.")
    parser.add_argument(
        "--movement",
        choices=["core", "upper", "lower", "full"],
        default="core",
        help=(
            "Movement focus for AUTO landmark selection. "
            "Use 'core' for crunches, sit-ups, leg raises (default)."
        ),
    )
    parser.add_argument(
        "--landmark",
        default="AUTO",
        help=(
            "Pose landmark to track (default: AUTO). "
            "AUTO picks the best joint for the selected --movement type."
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
        help="Use valleys or peaks as rep boundaries. Default: valley.",
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
            movement=args.movement,
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
