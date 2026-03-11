from PIL import Image
import os

files = {
    "app/src/main/res/mipmap-xxhdpi/ic_launcher.png": (144, 144),
    "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png": (192, 192)
}

for path, size in files.items():
    try:
        print(f"Processing {path}...")
        img = Image.open(path)
        print(f"Original format: {img.format}, size: {img.size}")
        
        # Resize using LANCZOS (available directly on Image in some versions)
        # If not, consistent fallback
        resample_method = getattr(Image, 'LANCZOS', getattr(Image, 'ANTIALIAS', Image.BICUBIC))
        
        img = img.resize(size, resample_method)
        
        # Save as PNG
        img.save(path, "PNG")
        print(f"Saved {path} as PNG with size {size}")
    except Exception as e:
        print(f"Error processing {path}: {e}")
