#!/usr/bin/env python3
import os
import math
from PIL import Image, ImageDraw, ImageFilter

def create_banadb_icon(size, transparent_bg=False):
    scale = 4
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = s / 2.0, s / 2.0

    if not transparent_bg:
        # Background: deep cyber dark
        bg_color = (10, 14, 23, 255) # #0A0E17
        draw.rectangle([0, 0, s, s], fill=bg_color)

        # Subtle cyber grid
        grid_color = (21, 32, 51, 140)
        grid_step = s / 8.0
        for i in range(1, 8):
            x = i * grid_step
            draw.line([(x, 0), (x, s)], fill=grid_color, width=max(1, int(scale * 1.2)))
            draw.line([(0, x), (s, x)], fill=grid_color, width=max(1, int(scale * 1.2)))

    # Radial cyber glow behind emblem
    glow_radius = s * 0.32
    glow_img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_img)
    glow_draw.ellipse(
        [cx - glow_radius, cy - glow_radius, cx + glow_radius, cy + glow_radius],
        fill=(0, 229, 255, 50) # Cyber cyan glow
    )
    glow_img = glow_img.filter(ImageFilter.GaussianBlur(radius=scale * 12))
    img.paste(glow_img, (0, 0), glow_img)
    draw = ImageDraw.Draw(img)

    # Hexagonal badge (hex_r = s * 0.28 to give breathing room in round icons)
    hex_r = s * 0.28
    hex_points = []
    for i in range(6):
        angle = math.radians(60 * i - 30)
        hx = cx + hex_r * math.cos(angle)
        hy = cy + hex_r * math.sin(angle)
        hex_points.append((hx, hy))

    # Dark translucent fill for hexagon
    hex_fill = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    hex_fill_draw = ImageDraw.Draw(hex_fill)
    hex_fill_draw.polygon(hex_points, fill=(16, 26, 42, 230))
    img.paste(hex_fill, (0, 0), hex_fill)
    draw = ImageDraw.Draw(img)

    # Hexagon outer border (Cyber Cyan glow)
    border_width = int(scale * 3.5)
    hex_points_closed = hex_points + [hex_points[0]]
    for i in range(6):
        draw.line([hex_points_closed[i], hex_points_closed[i+1]], fill=(0, 229, 255, 255), width=border_width)

    # Corner nodes on hexagon
    node_r = scale * 4.0
    for (hx, hy) in hex_points:
        draw.ellipse([hx - node_r, hy - node_r, hx + node_r, hy + node_r], fill=(0, 229, 255, 255))
        draw.ellipse([hx - node_r*0.5, hy - node_r*0.5, hx + node_r*0.5, hy + node_r*0.5], fill=(255, 255, 255, 255))

    # Inner Logo Motif:
    # Stylized ADB Wireless & Lightning Symbol
    bolt_pts = [
        (cx + s * 0.035, cy - s * 0.17),  # top point
        (cx - s * 0.10, cy + s * 0.015), # inner left crook
        (cx - s * 0.015, cy + s * 0.015), # center cross
        (cx - s * 0.065, cy + s * 0.17), # bottom point
        (cx + s * 0.085, cy - s * 0.025), # inner right crook
        (cx + s * 0.00, cy - s * 0.025), # center cross upper
    ]
    draw.polygon(bolt_pts, fill=(0, 229, 255, 255))

    # Core highlight inside lightning bolt
    bolt_core_pts = [
        (cx + s * 0.028, cy - s * 0.135),
        (cx - s * 0.065, cy + s * 0.00),
        (cx - s * 0.015, cy + s * 0.00),
        (cx - s * 0.045, cy + s * 0.13),
        (cx + s * 0.060, cy - s * 0.01),
        (cx + s * 0.00, cy - s * 0.01),
    ]
    draw.polygon(bolt_core_pts, fill=(255, 255, 255, 240))

    # ADB Connection Nodes & Signals (Left & Right WiFi/Debug Arcs)
    # Left Arc
    arc_box_1 = [cx - s * 0.22, cy - s * 0.14, cx + s * 0.22, cy + s * 0.14]
    draw.arc(arc_box_1, start=140, end=220, fill=(0, 229, 255, 220), width=int(scale * 2.2))
    arc_box_2 = [cx - s * 0.18, cy - s * 0.10, cx + s * 0.18, cy + s * 0.10]
    draw.arc(arc_box_2, start=145, end=215, fill=(0, 229, 255, 160), width=int(scale * 1.8))

    # Right Arc
    draw.arc(arc_box_1, start=320, end=40, fill=(0, 229, 255, 220), width=int(scale * 2.2))
    draw.arc(arc_box_2, start=325, end=35, fill=(0, 229, 255, 160), width=int(scale * 1.8))

    # Terminal prompt underscore beside bottom right
    cursor_w = scale * 8
    cursor_h = scale * 2.8
    draw.rectangle(
        [cx + s * 0.09, cy + s * 0.11, cx + s * 0.09 + cursor_w, cy + s * 0.11 + cursor_h],
        fill=(0, 229, 255, 255)
    )

    final_img = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_img

def create_round_icon(size):
    square = create_banadb_icon(size)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([0, 0, size, size], fill=255)
    round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_img.paste(square, (0, 0), mask)
    return round_img

def main():
    res_dir = "/home/baner/Projects/banadb/app/src/main/res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    # Ensure directories exist
    for d, sz in densities.items():
        folder = os.path.join(res_dir, d)
        os.makedirs(folder, exist_ok=True)
        # Standard icon
        icon = create_banadb_icon(sz)
        icon.save(os.path.join(folder, "ic_launcher.png"))
        # Round icon
        round_icon = create_round_icon(sz)
        round_icon.save(os.path.join(folder, "ic_launcher_round.png"))
        print(f"Generated {d} ({sz}x{sz})")

    # In-app logo resource in drawable
    drawable_dir = os.path.join(res_dir, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)
    logo_512 = create_banadb_icon(512)
    logo_512.save(os.path.join(drawable_dir, "ic_banadb_logo.png"))

    # Adaptive icons for anydpi-v26
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    
    # Save foreground with transparent background for adaptive
    fg = create_banadb_icon(432, transparent_bg=True)
    # Background for adaptive
    scale = 4
    s = 432 * scale
    bg_img = Image.new("RGBA", (s, s), (10, 14, 23, 255))
    bg_draw = ImageDraw.Draw(bg_img)
    grid_color = (21, 32, 51, 140)
    grid_step = s / 8.0
    for i in range(1, 8):
        x = i * grid_step
        bg_draw.line([(x, 0), (x, s)], fill=grid_color, width=max(1, int(scale * 1.2)))
        bg_draw.line([(0, x), (s, x)], fill=grid_color, width=max(1, int(scale * 1.2)))
    bg = bg_img.resize((432, 432), Image.Resampling.LANCZOS)
    
    drawable_dir = os.path.join(res_dir, "drawable")
    fg.save(os.path.join(drawable_dir, "ic_launcher_foreground.png"))
    bg.save(os.path.join(drawable_dir, "ic_launcher_background.png"))
    
    # Brain artifact preview
    out_dir = "/home/baner/.gemini/antigravity/brain/ca9245f5-8cd0-4343-a1df-3275fcf65db8"
    logo_512.save(os.path.join(out_dir, "banadb_logo_512.png"))
    round_512 = create_round_icon(512)
    round_512.save(os.path.join(out_dir, "banadb_logo_round_512.png"))
    print("All mipmap, adaptive, and logo assets generated successfully!")

if __name__ == "__main__":
    main()
