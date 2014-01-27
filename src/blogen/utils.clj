(ns blogen.utils
  )

(defn post-last-modified
  "Return the last modification date, minor or major."
  [post]
  (-> post :history first :date))

(defn post-last-major-modified
  "Get the latest major rev date of a post. If there are no major
  revisions, take earliest revision."
  [post]
  (let [revs (:history post)
        major-revs (filter :major? revs)]
    (:date
     (if (seq major-revs)
       (first major-revs)
       (last revs)))))