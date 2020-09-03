# SeamCarver
![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/AlexLatz/SeamCarver?include_prereleases&style=flat)
![GitHub](https://img.shields.io/github/license/AlexLatz/SeamCarver)

SeamCarver is a JavaFX application that uses the Seam Carver algorithm to scale images without distortion.

## Demonstration
To find each seam, the program computes the "energy" value of each pixel as seen in the [MERL research paper](http://graphics.cs.cmu.edu/courses/15-463/2012_fall/hw/proj3-seamcarving/imret.pdf).

Next, a topological sort finds the smallest energy paths from opposite sides, and removes or adds onto it.
![Shrink and Widen](shrinkwiden.gif)

The main difference between this implementation and many others is its ability to bulk-generate the seams.

The algorithm is designed to generate multiple seams and store past additions and deletions in order to create more coherent images.
![Diagonal Shrink and Widen](diagonalshrinkwiden.gif)

## Installation
Download the latest version of SeamCarver from the [releases page](https://github.com/AlexLatz/SeamCarver/releases/latest) and run the application (Mac, Linux) or the installer (Windows).

## Usage
Open an image using the menu, or use the default image.

Then resize the window to live-resize the image, or use the menu to set a specific size in pixels.

You can then export the image to JPEG, PNG, BMP, or GIF formats.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Licence
[GNU GPLv3](https://choosealicense.com/licenses/gpl-3.0/)
