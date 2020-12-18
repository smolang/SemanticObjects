;;; smol-mode.el --- Major mode for the Smol language -*- lexical-binding: t; -*-

(defvar smol-mode-syntax-table (copy-syntax-table)
  "Syntax table for `smol-mode'.")
(modify-syntax-entry ?/   ". 124" smol-mode-syntax-table)
(modify-syntax-entry ?*   ". 23b" smol-mode-syntax-table)
(modify-syntax-entry ?\n  ">"     smol-mode-syntax-table)
(modify-syntax-entry ?\^m ">"     smol-mode-syntax-table)

(defconst smol-keywords
  (regexp-opt
   '("skip" "return" "if" "fi" "then" "new" "else" "while" "do" "main" "print" "class" "end" "extends" "access" "derive" "breakpoint" "rule")
   'words)
  "List of SMOL keywords.")

(defconst smol-constants
  (regexp-opt
   '("True" "False" "null" "this")
   'words)
  "List of SMOL constants.")

(defvar smol-font-lock-defaults
  (list
   (cons smol-keywords font-lock-keyword-face)
   (cons smol-constants font-lock-constant-face))
  "Font lock information for SMOL.")

(define-derived-mode smol-mode prog-mode "SMOL"
  "Major mode for editing SMOL files."
  :group 'smol
  (set (make-local-variable 'comment-use-syntax) t)
  (set (make-local-variable 'comment-start) "//")
  (set (make-local-variable 'comment-end) "")
  (set (make-local-variable 'comment-start-skip) "//+\\s-*")
  (setq font-lock-defaults (list 'smol-font-lock-defaults)))

;;;###autoload
(add-to-list 'auto-mode-alist '("\\.smol\\'" . smol-mode))
