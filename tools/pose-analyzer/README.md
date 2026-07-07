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
- Dependencies: `mediapipe`, `opencv-python-headless`, `numpy`, `scipy`

## Setup

### Linux / macOS (normal)

```bash
cd /home/abdul/RiderProjects/Clip-It/tools/pose-analyzer

python3 -m venv venv
source venv/bin/activate

pip install --upgrade pip
pip install -r requirements.txt
```

### NixOS (recommended)

On NixOS, a plain `venv` often fails with `libstdc++.so.6` / NumPy import errors.
Use the provided Nix shell instead:

```bash
cd /home/abdul/RiderProjects/Clip-It/tools/pose-analyzer

# One-time: remove broken venv if you created one outside nix-shell
rm -rf venv

nix-shell shell.nix
```

Inside the shell:

```bash
python analyze_video.py --input /path/to/video.mp4 --landmark LEFT_WRIST --debug
```

Or without entering the shell:

```bash
chmod +x run.sh
./run.sh --input /path/to/video.mp4 --landmark LEFT_WRIST --debug
```

The Nix shell creates `.venv-nix/` with working native libraries (`gcc` stdlib in `LD_LIBRARY_PATH`).

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
- **Bicep curls / upper body**: try `LEFT_WRIST` or `RIGHT_WRIST`, not `LEFT_KNEE`
- **Pull-ups / overhead press**: try `RIGHT_SHOULDER` or `LEFT_WRIST`
- If reps are over-counted, increase `--min-rep-duration-ms`
- Use `--debug` to validate JSON before importing into Clip-It

## Troubleshooting

### `libxcb.so.1` / `libstdc++.so.6` (NixOS)

Your venv was created without Nix runtime libraries. Fix:

```bash
rm -rf venv .venv-nix
nix-shell shell.nix
pip uninstall -y opencv-python opencv-python-headless 2>/dev/null || true
pip install -r requirements.txt
python analyze_video.py --input /path/to/video.mp4 --debug
```

We use `opencv-python-headless` (no GUI/X11) for CLI video processing on NixOS.

## Branch

This tool is developed on branch `feature/pose-estimation-tool` and is intentionally separate from the Android app code.
