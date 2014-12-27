(ns med-org.core
  (:gen-class)
  (:use exif-processor.core)
  (:require [clojure.string :refer [lower-case split blank? join upper-case]]
            [me.raynes.fs :refer [find-files absolute mkdir copy copy+ exists? parent delete]]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.tools.cli :as cli]))

(def exif-date-time-format (f/formatter "yyyy:MM:dd HH:mm:ss"))
(def exif-supported-formats ["jpg" "tif"])

;;Not an exhaustive list of media files
(def media-files
  (apply conj exif-supported-formats ["png" "bmp" "gif" "mp4" "avi" "3gp" "wav" "mkv" "mts" "raw"])
  #_(apply conj exif-supported-formats ["png" "bmp"]))

(defn exif-supported-format? [file]
  (if (some #(.endsWith (.getName file) %) exif-supported-formats) true false))

(defn get-files [dir]
  (find-files dir (re-pattern
                    (apply str
                           (drop-last (reduce #(concat %1 ".*?" %2 "|") "(?i)" media-files))))))

(defn get-date-from-file-or-dir [file format pattern]
  "Tries to match the regex pattern with the file name, if it matches,
  then it converts it into a date"
  (let [name (re-find (re-pattern pattern) (.getName file))]
    (if name (f/parse (f/formatter format) (first name)))))


(defn file-or-dir-or-modified-date [file options]
  "Returns a vector of [date suffix]

  Tries the file date first, if not available tries the directory date and if not
  available still gets the modified date. It also provides respective suffix to be used. This
  also serves the purpose of knowing which date is being returned"
  (let [file-name-date
        (get-date-from-file-or-dir file (:file-date-format options) (:file-date-pattern options))]
    (if file-name-date
      [file-name-date "_fln"]
      (let [parent-dir-date
            (get-date-from-file-or-dir (parent file) (:dir-date-format options) (:dir-date-pattern options))]
        (if parent-dir-date
          [parent-dir-date "_dir"]
          [(c/from-long (.lastModified file)) "_app"])))))

(defn get-exif-from-file [file]
  "Reads creation date from EXIF file, in case of exception returns a blank"
  (try (exif-for-file file)
       (catch Exception e
         #_(println "Couldn't read EXIF for the file : "
                  (.getName file) " -> "(.getMessage e))
         ({}))))


(defn get-date [file options]
  "Determines the file creation date in the following order
    1. Creation Date from EXIF
    2. Creation date from the file name.
    3. Creation date from the directory.
    4. Finally, if above all fails, then it gets from the last modifed date of the file."
  (if (exif-supported-format? file)
    (let [exif-date-str (get (get-exif-from-file file) "Date/Time")]
      (if exif-date-str
        [(f/parse exif-date-time-format exif-date-str) "_exf"]
        (file-or-dir-or-modified-date file options)))
    (file-or-dir-or-modified-date file options)))

(defn get-dst-dir [date dest-dir]
  "Creates the destination directory in the following format - \"dest-dir\"/year/year_month/year_month_day"
  (str dest-dir "/"
       (t/year date) "/"
       (str (t/year date) "_" (f/unparse (f/formatter "MM") date)) "/"
       (str (t/year date) "_" (f/unparse (f/formatter "MM") date) "_"
            (f/unparse (f/formatter "dd") date))))

(defn append-suffix[filename-str suffix]
  (let [spl (split filename-str #"\.")]
    (str (first spl) suffix "." (second spl))))

(defn move-file [file options]
  "Determines the media creation date, creates the destination directory structure
  and moves the file to the destination"
    (let [date-and-suffix (get-date file options)
          dst-dir (get-dst-dir (first date-and-suffix) (:dest-dir options))
          dst-file (str dst-dir "/"
                        (append-suffix (.getName file) (second date-and-suffix)))]
      (if (not (exists? (str dst-dir "/" (.getName file))))
        (copy+ file dst-file))
      (if (not (:copy options))
         (delete (.getPath file)))))

(defn group-files [files]
  "Groups the files by file types"
  (group-by
    #(lower-case (apply str (take-last 3 (.getName %)))) files))

(defn print-stats [files]
  "Prints stats regarding the file count and file types"
  (let [groups (group-files files)]
    (println "Total files processed: " (count files))
    (->> (map #(str " " (upper-case %)  " files: " (count (get groups %))) (keys groups))
         (join \newline)
         (println))))


(defn organize [options]
  "The program finds all the media files (such as JPG, PNG, BMP, GIF, MP4 etc) from the \"src-dir\"
  and moves them to \"dest-dir\". While moving the files it also organizes the media by it's creation
  date in the following structure \"dest-dir\"/year/year_month/year_month_day."
  (let [files (get-files (:src-dir options))]
    (println "Processing " (count files) " files...")
    (doall (pmap #(move-file % options) files))
    (print-stats files)
    (if (not (:copy options))
        (pmap #(delete (.getPath %)) files))
    (shutdown-agents)))

(def cli-options

  [["-s" "--src-dir SOURCE-DIR" "Path to source directory where the media is available"
    :validate [#(exists? %) "Source directory doesn't exist"]]

   ["-d" "--dest-dir DEST-DIR" "Path to destination directory where the media should be moved"
    :validate [#(not (blank? %)) "Destination directory cannot blank"]]

   ["-ff" "--file-date-format DATE-FORMAT" "(Optional) In case the date could not be found in the EXIF,
                                                    then please provide a date pattern available in the media
                                                    file name. Default yyyyMMdd"
    :default "yyyyMMdd"
    :default-desc ""
    :validate [#(not (blank? %)) "Date format cannot blank"]]

   ["-fp" "--file-date-pattern DATE-REGEX-PATTERN" "(Optional) The corresponding Regex pattern for the above
                                                    format, to capture and identify the date from the  media file name.
                                                    Default pattern (19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])"
    :default "(19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])"
    :default-desc ""
    :validate [#(not (blank? %)) "Date pattern cannot blank"]]

   ["-df" "--dir-date-format DATE-FORMAT" "(Optional) In case there is no date available in EXIF and in the
                                                    filename, then provide a date pattern of your directory name.
                                                    Default yyyy-MM-dd"
    :default "yyyy-MM-dd"
    :default-desc ""
    :validate [#(not (blank? %)) "Date format cannot blank"]]

   ["-dp" "--dir-date-pattern DATE-REGEX-PATTERN" "(Optional) The corresponding Regex pattern for the above format,
                                                    to capture and identify the date from the directory name.
                                                    Default pattern (19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])"
    :default "(19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])"
    :default-desc ""
    :validate [#(not (blank? %)) "Date pattern cannot blank"]]

   ["-c" "--copy" "(Optional) Copy files. If not provided, the files are MOVED"]

   ["-h" "--help" "To see this usage summary"]])

(defn usage [options-summary]
  (->> [""
        "The program finds all the media files (such as JPG, PNG, BMP, GIF, MP4 etc) from the \"src-dir\" and moves them to \"dest-dir\". While moving the files it also organizes the media by it's creation date in the following structure \"dest-dir\"/year/year_month/year_month_day. The program determines the media creation date in the following order.."
        " 1. Extract Creation Date from EXIF"
        " 2. Extract the creation date from the file name. (Options ff and fp)"
        " 3. Extract the creation date from the directory. (Options df and dp)"
        " 4. Finally, if above all fails, then it gets from the last modifed date of the file."
        ""
        "Usage: java -jar <name> [options]"
        ""
        "Options:"
        options-summary
        ""
        ]
       (join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))


(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)]
    (cond
      (:help options)
        (println (usage summary))
      errors
        (println (error-msg errors))
      (:src-dir options)
         (if (not (:dest-dir options))
           (println (error-msg (conj errors "Please provide \"-d or --dest-dir\"")))
           (organize options))
      (:dest-dir options)
         (println (error-msg (conj errors "Please provide \"-s or --src-dir\""))))))

