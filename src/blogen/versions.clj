(ns blogen.versions
  "Using the underlying version control system we can manage post
  revision histories and provide all kinds of information.

  DEFINITION: A [revision] history of a file is a seq of revisions,
  ordered by time desc. The topmost is the newest and the current
  revision.

  DEFINITION: A revision is a map with keys :hash, :date and :note."
  (:require [clj-time.format])
  )

;;; git log --format="%%HASH: %h %n%%DATE: %aD %n%%NOTE: %s %n%%%%"

(def git-log-date-format
  (clj-time.format/formatters :rfc822))

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
  (seq [(read-revision "")]))