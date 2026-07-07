# Pose Analyzer (Clip-It Game Mode)

Standalone Python tool that analyzes workout videos with **MediaPipe Pose**, detects repetition segments from a tracked body landmark, and exports JSON files for Clip-It's upcoming Game Mode.

## Output format

```json
{
  "videoFile": "squat_session.mp4",
  "reps": [
    { "start": 1200, "end": 2800 },
    { "start": 3100, "end": 4700 }
  ]
}
```

- `videoFile`: source video filename
- `reps[].start` / `reps[].end`: timestamps in **milliseconds**

Files are written to `tools/pose-analyzer/output/<videoname>.json`.

## Requirements

- Python 3.10+
- Dependencies: `mediapipe`, `opencv-python`, `numpy`, `scipy`

## Setup

```bash
cd /home/abdul/RiderProjects/Clip-It/tools/pose-analyzer

python3 -m venv venv
source venv/bin/activate

pip install --upgrade pip
pip install -r requirements.txt
```

## Usage

Basic analysis (default landmark: `LEFT_KNEE`):

```bash
python analyze_video.py --input /path/to/video.mp4
```

Track a different landmark:

```bash
python analyze_video.py \
  --input /path/to/video.mp4 \
  --landmark RIGHT_SHOULDER
```

Tune rep detection:

```bash
python analyze_video.py \
  --input /path/to/video.mp4 \
  --landmark LEFT_KNEE \
  --min-rep-duration-ms 800 \
  --rep-boundary valley
```

Debug mode (skeleton + rep markers):

```bash
python analyze_video.py \
  --input /path/to/video.mp4 \
  --landmark LEFT_KNEE \
  --debug
```

This writes:

- `output/<videoname>.json`
- `output/<videoname>_debug.mp4`

## CLI options

| Flag | Default | Description |
|------|---------|-------------|
| `--input` | required | Path to input video |
| `--landmark` | `LEFT_KNEE` | MediaPipe landmark name |
| `--min-rep-duration-ms` | `500` | Minimum spacing between rep boundaries |
| `--rep-boundary` | `valley` | `valley` or `peak` extrema as rep boundaries |
| `--output-dir` | `./output` | Output directory |
| `--debug` | off | Render annotated debug video |

### Supported landmarks

`LEFT_KNEE`, `RIGHT_KNEE`, `LEFT_HIP`, `RIGHT_HIP`, `LEFT_SHOULDER`, `RIGHT_SHOULDER`, `LEFT_WRIST`, `RIGHT_WRIST`, `LEFT_ANKLE`, `RIGHT_ANKLE`, `NOSE`

## How rep detection works

1. MediaPipe Pose tracks the selected landmark per frame.
2. The landmark **Y coordinate** (normalized, 0 = top, 1 = bottom) is collected over time.
3. The signal is smoothed with `scipy.signal.savgol_filter`.
4. Local extrema are found via `scipy.signal.find_peaks` (peaks + inverted peaks for valleys).
5. Consecutive boundaries of the chosen type (`valley` or `peak`) form rep segments `{start, end}`.

## Tips

- **Squats / lunges**: try `LEFT_KNEE` or `RIGHT_KNEE`, `--rep-boundary valley`
- **Pull-ups / overhead press**: try `RIGHT_SHOULDER` or `LEFT_WRIST`
- If reps are over-counted, increase `--min-rep-duration-ms`
- Use `--debug` to validate JSON before importing into Clip-It

## Branch

This tool is developed on branch `feature/pose-estimation-tool` and is intentionally separate from the Android app code.
