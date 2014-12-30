# med-org

This is a command line utility which helps you in organizing your media files chronologically.

It finds all the media files (such as JPG, PNG, BMP, GIF, MP4 etc) from the "src-dir" and moves them to "dest-dir". While moving the files it  organizes the media by it's creation date in the following directory structure - \"dest-dir\"/year/year_month/year_month_day.

The program determines the file creation date in the following order..
1. Creation date from EXIF
2. Creation date from the file name (if it is based on date).
3. Creation date from the directory in which the file is present (if it is based on date)
4. Finally, if above all fails, the last modifed date of the file.


## Installation

Download the med-org-0.1.0-standalone.jar from https://github.com/assatishkumar/med-org/tree/master/download/med-org-0.1.0-standalone.jar

## Usage

This utility should be run from commandline. Requires JDK 6 or above

    $ java -jar med-org-0.1.0-standalone.jar -s /path/to/your/media/files -d /path/to/destination/dir

## Options
```
The utility is configurable and it accepts the following options

Options:
  -s, --src-dir SOURCE-DIR                       Path to source directory where the media is available
  -d, --dest-dir DEST-DIR                        Path to destination directory where the media should be moved
  -ff, --file-date-format DATE-FORMAT            (Optional) In case the date could not be found in the EXIF,
                                                    then please provide a date pattern available in the media
                                                    file name. Default yyyyMMdd
  -fp, --file-date-pattern DATE-REGEX-PATTERN    (Optional) The corresponding Regex pattern for the above
                                                    format, to capture and identify the date from the  media file name.
                                                    Default pattern (19|20)\d\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])
  -df, --dir-date-format DATE-FORMAT             (Optional) In case there is no date available in EXIF and in the
                                                    filename, then provide a date pattern of your directory name.
                                                    Default yyyy-MM-dd
  -dp, --dir-date-pattern DATE-REGEX-PATTERN     (Optional) The corresponding Regex pattern for the above format,
                                                    to capture and identify the date from the directory name.
                                                    Default pattern (19|20)\d\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])
  -c, --copy                                     (Optional) Copy files. If not provided, the files are MOVED
  -h, --help                                     To see this usage summary
```

## Examples

### To move files

$ java -jar med-org-0.1.0-standalone.jar -s /path/to/your/media/files -d /path/to/destination/dir

### To copy files

$ java -jar med-org-0.1.0-standalone.jar -s /path/to/your/media/files -d /path/to/destination/dir -c

### To see options (or help)

$ java -jar med-org-0.1.0-standalone.jar -h

## License

Copyright © 2014 Satish Srinivasan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
