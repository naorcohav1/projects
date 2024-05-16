#lang racket
(require rackunit)
(require "ex5.rkt")

(define id (lambda (x) x))

;; Q1a
(check-equal? (append$ '(1 2) '(3 4) id) '(1 2 3 4) "incorrect append$ 1")
(check-equal? (append$ '() '(3 4) id)  '(3 4) "incorrect append$ 2")
(check-equal? (append$ '() '() id)  '() "incorrect append$ 3")

;; Q1b
(check-equal? (equal-trees$ '(1 (2) (3 9)) '(7 (2) (3 5)) id id) '((1 . 7) ((2 . 2)) ((3 . 3) (9 . 5))) "incorrect equal-trees$ 1")
(check-equal? (equal-trees$ '(1 (2) (3 9)) '(1 (2) (3 9)) id id) '((1 . 1) ((2 . 2)) ((3 . 3) (9 . 9))) "incorrect equal-trees$ 2")
(check-equal? (equal-trees$ '(1 2 (3 9)) '(1 (2) (3 9)) id id) '(2 2) "incorrect equal-trees$ 3")
(check-equal? (equal-trees$ '(1 2 (3 9)) '(1 (3 4)) id id) '(2 3 4) "incorrect equal-trees$ 4")
(check-equal? (equal-trees$ '(1 (2) ((4 5))) '(1 (#t) ((4 5))) id id) '((1 . 1) ((2 . #t)) (((4 . 4) (5 . 5)))) "incorrect equal-trees$ 5")


;;; Q2.1
(check-equal? (take (as-real 4) 8) '(4 4 4 4 4 4 4 4) "incorrect as-real 1")
(check-equal? (take (as-real 3) 2) '(3 3) "incorrect as-real 2")
(check-equal? (take (++ (integers-from 0) (integers-from 10)) 5) '(10 12 14 16 18) "incorrect ++ 1")
(letrec ((r3 (cons-lzl 3 (lambda () r3))) (r5 (cons-lzl 5 (lambda () r5))))
  (check-equal? (take (++ r3 r5) 4) '(8 8 8 8) "incorrect ++ 2")
)
(check-equal? (take (-- (integers-from 10) (integers-from 2)) 5) '(8 8 8 8 8) "incorrect -- 1")
(letrec ((r10 (cons-lzl 10 (lambda () r10))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (-- r10 r2) 4) '(8 8 8 8) "incorrect -- 2")
)
(check-equal? (take (** (integers-from 10) (integers-from 2)) 4) '(20 33 48 65) "incorrect ** 1")
(letrec ((r4 (cons-lzl 4 (lambda () r4))))
  (check-equal? (take (** r4 (integers-from 0)) 6) '(0 4 8 12 16 20) "incorrect ** 2")
)
(letrec ((r3 (cons-lzl 3 (lambda () r3))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (** r3 r2) 8) '(6 6 6 6 6 6 6 6) "incorrect ** 3")
)
(check-equal? (take (// (integers-from 10) (integers-from 1)) 5) '(10/1 11/2 12/3 13/4 14/5) "incorrect // 1")
(letrec ((r10 (cons-lzl 10 (lambda () r10))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (// r10 r2) 4) '(5 5 5 5) "incorrect // 2")
)
(letrec ((r1 (cons-lzl 1 (lambda () r1))))
  (check-equal? (take (// (integers-from 0) r1) 8) (take (integers-from 0) 8) "incorrect // 3")
)

;;; Q2.2.a
(letrec ((r2 (cons-lzl 2 (lambda () r2))) (decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 2)) decay)))))
  (check-equal? (take (head (sqrt-with r2 decay)) 8) (take decay 8) "incorrect sqrt-with 1")
)
(letrec ((r2 (cons-lzl 2 (lambda () r2))) (decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 2)) decay)))))
  (check-equal? (take (head (sqrt-with decay r2)) 8) (take r2 8) "incorrect sqrt-with 2")
)
(letrec ((decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 2)) decay)))))
  (check-equal? (take (head (sqrt-with decay decay)) 8) (take decay 8) "incorrect sqrt-with 3")
)
(letrec ((decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 2)) decay)))))
  (check-equal? (take (head (tail (sqrt-with decay decay))) 4) '(1 3/4 5/8 9/16) "incorrect sqrt-with 4")
)

;;; Q2.2.b
(letrec ((just8 (cons-lzl 8 (lambda () just8))))
  (letrec ((justjust8 (cons-lzl just8 (lambda () justjust8))))
    (check-equal? (take (diag justjust8) 5) '(8 8 8 8 8) "incorrect diag 1")
  )
)
(letrec ((r (cons-lzl (integers-from 0) (lambda () (map-lzl (lambda (lzl) (map-lzl (lambda (x) (* x 10)) lzl)) r)))))
  (check-equal? (take (diag r) 5) '(0 10 200 3000 40000) "incorrect diag 2")
)

;;; Q2.2.c
(letrec ((r4 (cons-lzl 4.0 (lambda () r4))))
  (check-equal? (< (abs (- (nth (rsqrt r4) 6) 2.0)) 0.001) #t "incorrect rsqrt 1")
)
(letrec ((decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 10.0)) decay)))))
  (check-equal? (< (nth (rsqrt decay) 6) 0.1) #t "incorrect rsqrt 2")
)