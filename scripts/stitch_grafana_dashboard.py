import argparse
import re
from pathlib import Path

from PIL import Image


SCROLL_RE = re.compile(r"scroll(\d+)", re.IGNORECASE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Stitch Grafana viewport captures into one long dashboard PNG."
    )
    parser.add_argument("--input-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--header-height", type=int, required=True)
    parser.add_argument("--body-x", type=int, required=True)
    parser.add_argument("--body-y", type=int, required=True)
    parser.add_argument("--body-width", type=int, required=True)
    parser.add_argument("--body-height", type=int, required=True)
    parser.add_argument(
        "--scroll-height",
        type=int,
        default=None,
        help="Full scrollable body height. Defaults to max(scroll offset + body height).",
    )
    return parser.parse_args()


def scroll_offset(path: Path, fallback: int) -> int:
    match = SCROLL_RE.search(path.stem)
    if match:
        return int(match.group(1))
    return fallback


def load_captures(input_dir: Path, body_height: int) -> list[tuple[Path, int]]:
    files = sorted(input_dir.glob("*.png"))
    if not files:
        raise SystemExit(f"No PNG captures found in {input_dir}")

    captures = [
        (path, scroll_offset(path, index * body_height))
        for index, path in enumerate(files)
    ]
    return sorted(captures, key=lambda item: item[1])


def main() -> None:
    args = parse_args()
    captures = load_captures(args.input_dir, args.body_height)

    first_image = Image.open(captures[0][0]).convert("RGB")
    scroll_height = args.scroll_height
    if scroll_height is None:
        scroll_height = max(offset + args.body_height for _, offset in captures)

    output_width = first_image.width
    output_height = args.header_height + scroll_height
    stitched = Image.new("RGB", (output_width, output_height), (17, 18, 23))

    header = first_image.crop((0, 0, output_width, args.header_height))
    stitched.paste(header, (0, 0))

    for path, offset in captures:
        image = Image.open(path).convert("RGB")
        body = image.crop(
            (
                args.body_x,
                args.body_y,
                args.body_x + args.body_width,
                args.body_y + args.body_height,
            )
        )
        visible_height = min(args.body_height, scroll_height - offset)
        if visible_height <= 0:
            continue
        stitched.paste(
            body.crop((0, 0, args.body_width, visible_height)),
            (args.body_x, args.header_height + offset),
        )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    stitched.save(args.output)
    print(args.output)
    print(f"{stitched.width}x{stitched.height}")


if __name__ == "__main__":
    main()
