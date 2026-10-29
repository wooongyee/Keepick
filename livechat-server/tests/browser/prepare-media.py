"""Generate local synthetic cameras and Playwright CLI launch configs."""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[3]
output = root / "output/playwright/keepick-media"
output.mkdir(parents=True, exist_ok=True)
for name, hue in [("a", 0), ("b", 120)]:
    camera = output / f"camera-{name}.y4m"
    subprocess.run([
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
        "-f", "lavfi", "-i", "testsrc2=size=640x360:rate=15",
        "-t", "4", "-vf", f"hue=h={hue}", "-pix_fmt", "yuv420p",
        "-f", "yuv4mpegpipe", str(camera),
    ], check=True)
    config = {
        "browser": {
            "browserName": "chromium",
            "launchOptions": {
                "channel": "chrome", "headless": False,
                "args": [
                    "--use-fake-device-for-media-stream",
                    "--use-fake-ui-for-media-stream",
                    f"--use-file-for-fake-video-capture={camera}",
                    "--autoplay-policy=no-user-gesture-required", "--mute-audio",
                ],
            },
            "contextOptions": {
                "permissions": ["camera", "microphone"],
                "viewport": {"width": 1440, "height": 1000},
            },
        },
    }
    (output / f"config-{name}.json").write_text(json.dumps(config, indent=2) + "\n")
print(f"Prepared synthetic cameras and configs in {output}")
