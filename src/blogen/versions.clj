(ns blogen.versions
  "Using the underlying version control system we can manage post
  revision histories and provide all kinds of information.

  DEFINITION: A [revision] history of a file is a seq of revisions,
  ordered by time desc. The topmost is the newest and the current
  revision.

  DEFINITION: A revision is a map with keys :hash, :date and :note."
  (:require [clj-time.format]
            [me.raynes.conch :refer [programs]])
  (:use [blogen.config]))

(programs git)

;;; git log --format="%%HASH: %h %n%%DATE: %aD %n%%NOTE: %s %n%%%%"
;;; both --git-dir and --work-tree need to be specified.

(def git-arguments
  [(str "--git-dir=" original-dir "/.git/")
   (str "--work-tree=" original-dir)
   "log"
   "--format=%%HASH: %h %n%%DATE: %aD %n%%NOTE: %s %n%%%%"])

(def git-log-date-format
  (clj-time.format/formatters :rfc822))

;;; TODO: need to specify a minor/major-edit tick here.

(defn read-revision
  "Read a revision from git output."
  [rev-str]
  (let [lines (vec (.split rev-str "\\n"))
        pieces (for [l lines]
                 (re-matches #"^%([A-Z]+): (.+?)\s*$" l))
        rev (into {} (for [p pieces]
                       [(keyword (.toLowerCase (p 1)))
                        (p 2)]))
        rev (update-in rev [:date]
                       (partial clj-time.format/parse
                                git-log-date-format))]
    rev))

(defn history
  "Get and parse revision history of given file."
  [path]
  (-> (apply git (conj git-arguments path))
      (.split "\\n%%\\n")
      seq
      ((partial map read-revision))))