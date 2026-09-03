# localnh-read

an android reader for localnh cartridge (`.lnhc`) files.

select a folder containing cartridges. the app reads each cartridge manifest for the library, opens its native gallery view, and displays pages with a native image reader. cartridges are copied only to the app cache for fast random page access; the originals are never modified.

open this folder in android studio or build it with the same github actions setup used by the html reader project. the project targets android 15 and supports android 8.0 or newer.
