"""Generate Android launcher icons from the master application artwork."""

from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork" / "app_icon.png"
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}


def remove_corner_backdrop(image: Image.Image) -> Image.Image:
    """Make the near-black area connected to the four corners transparent."""
    image = image.convert("RGBA")
    pixels = image.load()
    width, height = image.size
    queue = deque(
        [(0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)]
    )
    visited = bytearray(width * height)

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if visited[index]:
            continue
        visited[index] = 1

        red, green, blue, alpha = pixels[x, y]
        if alpha == 0 or max(red, green, blue) >= 80:
            continue

        pixels[x, y] = (red, green, blue, 0)
        if x:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    return image


def resize(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.Resampling.LANCZOS)


def circular_icon(image: Image.Image, size: int) -> Image.Image:
    oversample = 4
    large_size = size * oversample
    icon = resize(image, large_size)
    mask = Image.new("L", (large_size, large_size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, large_size - 1, large_size - 1), fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    icon = icon.resize((size, size), Image.Resampling.LANCZOS)
    icon.putalpha(ImageChops.multiply(icon.getchannel("A"), mask))
    return icon


def adaptive_foreground(image: Image.Image, scale: float) -> Image.Image:
    canvas_size = round(108 * scale)
    artwork_size = round(72 * scale)
    artwork = resize(image, artwork_size)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    offset = (canvas_size - artwork_size) // 2
    canvas.alpha_composite(artwork, (offset, offset))
    return canvas


def main() -> None:
    source = remove_corner_backdrop(Image.open(SOURCE))

    for density, scale in DENSITIES.items():
        output_dir = RES / f"mipmap-{density}"
        output_dir.mkdir(parents=True, exist_ok=True)
        legacy_size = round(48 * scale)

        resize(source, legacy_size).save(
            output_dir / "ic_launcher.webp", "WEBP", lossless=True
        )
        circular_icon(source, legacy_size).save(
            output_dir / "ic_launcher_round.webp", "WEBP", lossless=True
        )
        adaptive_foreground(source, scale).save(
            output_dir / "ic_launcher_foreground.webp", "WEBP", lossless=True
        )


if __name__ == "__main__":
    main()
