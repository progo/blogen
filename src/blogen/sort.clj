(ns blogen.sort
  "Collect and implement various methods to sort the seq of posts."
  (:require
   [clj-time.core :as time])
  (:use
   [blogen.config]))

(defn by-creation-date
  "sort by the original creation date (not latest modification). Order
  is DESC."
  [post-a post-b]
  (apply time/after? (map :created [post-a post-b])))

;; We didn't need to sort those revisions within one history again.
;; Git has us covered. (sort-by :date time/after? revs)
(defn- newest-revision-date
  [post]
  (-> (:history post) first :date))

(defn- newest-major-revision-date
  "Get the latest major rev date of a post. If there are no major
  revisions, take earliest revision."
  [post]
  (let [revs (:history post)
        major-revs (filter :major? revs)]
    (:date
     (if (seq major-revs)
       (first major-revs)
       (last revs)))))

;;; Yup, seems super wet to me. All three funcs can be generalized
;;; over this pattern.

(defn by-latest-revision
  "Sort by most recent modifications. Order by desc."
  [post-a post-b]
  (apply time/after? (map newest-revision-date
                          [post-a post-b])))

(defn by-latest-major-revision
  "Sort by most recent major modifications. Order by desc."
  [post-a post-b]
  (apply time/after? (map newest-major-revision-date
                          [post-a post-b])))

(defn by-word-count
  ""
  [post-a post-b])