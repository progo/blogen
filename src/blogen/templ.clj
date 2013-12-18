(ns blogen.templ
  (:use
   [blogen.config])

  (:require
   [net.cgrand.enlive-html :as html]))

(defn- streamify 
  [path]
  (java.io.FileReader. path))
(defn- with-template-dir
  [filename]
  (str template-dir "/" filename))
(defn- from-template
  "Take just a file name and produce a FileReader object for enlive to
  use."
  [template-name]
  (streamify
   (with-template-dir template-name)))

(html/deftemplate post-template (from-template "post.html")
  [title content]
  [:title] (html/content title)
  [:#content] (html/substitute content))