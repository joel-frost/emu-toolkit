#!/bin/bash
# This script converts an SVG icon to various formats needed for cross-platform applications
# Prerequisites: ImageMagick, librsvg2-bin (for rsvg-convert)

# Create output directory
mkdir -p output

# Check if source SVG is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <path-to-svg>"
  exit 1
fi

SVG_FILE="$1"
OUTPUT_DIR="."

echo "Converting SVG to PNG in various sizes..."

# Convert SVG to PNG in various sizes
for size in 16 32 48 64 128 256 512; do
  echo "Creating ${size}x${size} PNG..."
  rsvg-convert -w $size -h $size "$SVG_FILE" -o "$OUTPUT_DIR/icon_$size.png"
done

# Create Windows ICO file
echo "Creating Windows ICO file..."
convert "$OUTPUT_DIR/icon_16.png" "$OUTPUT_DIR/icon_32.png" "$OUTPUT_DIR/icon_48.png" \
  "$OUTPUT_DIR/icon_64.png" "$OUTPUT_DIR/icon_128.png" "$OUTPUT_DIR/icon_256.png" \
  "$OUTPUT_DIR/icon.ico"

# For macOS ICNS (more complex)
echo "Creating macOS ICNS file..."

# Create iconset directory structure
mkdir -p "$OUTPUT_DIR/EmuToolkit.iconset"

# Copy and rename files according to macOS iconset format
cp "$OUTPUT_DIR/icon_16.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_16x16.png"
cp "$OUTPUT_DIR/icon_32.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_16x16@2x.png"
cp "$OUTPUT_DIR/icon_32.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_32x32.png"
cp "$OUTPUT_DIR/icon_64.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_32x32@2x.png"
cp "$OUTPUT_DIR/icon_128.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_128x128.png"
cp "$OUTPUT_DIR/icon_256.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_128x128@2x.png"
cp "$OUTPUT_DIR/icon_256.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_256x256.png"
cp "$OUTPUT_DIR/icon_512.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_256x256@2x.png"
cp "$OUTPUT_DIR/icon_512.png" "$OUTPUT_DIR/EmuToolkit.iconset/icon_512x512.png"

# Try to create ICNS file using Linux tools first
if command -v png2icns &> /dev/null; then
  echo "Creating ICNS file using png2icns..."
  # png2icns requires individual files for each size
  png2icns "$OUTPUT_DIR/icon.icns" "$OUTPUT_DIR/icon_16.png" "$OUTPUT_DIR/icon_32.png" \
    "$OUTPUT_DIR/icon_48.png" "$OUTPUT_DIR/icon_64.png" "$OUTPUT_DIR/icon_128.png" \
    "$OUTPUT_DIR/icon_256.png" "$OUTPUT_DIR/icon_512.png"
  echo "ICNS file created using png2icns"
# Fall back to macOS iconutil if available
elif command -v iconutil &> /dev/null; then
  iconutil -c icns "$OUTPUT_DIR/EmuToolkit.iconset" -o "$OUTPUT_DIR/icon.icns"
  echo "ICNS file created using iconutil"
else
  echo "Neither png2icns nor iconutil found. You'll need to install icnsutils package or create the ICNS file manually."
  echo "On Ubuntu/Debian: sudo apt-get install icnsutils"
  echo "On Fedora: sudo dnf install libicns-utils"
  echo "Alternatively, use a service like https://cloudconvert.com/ to convert PNG to ICNS format."
fi

# For Linux, typically just use the PNG files directly
echo "For Linux, use the 128x128 PNG file."
cp "$OUTPUT_DIR/icon_128.png" "$OUTPUT_DIR/icon.png"

echo "Icon conversion complete!"
echo "Output files are in the '$OUTPUT_DIR' directory."
echo "=== Instructions ==="
echo "1. Move the generated icons to 'src/main/resources/icons/' in your project."
echo "2. Update your application code to use these icons."
echo "3. For macOS, if iconutil wasn't available, convert the iconset to .icns manually."