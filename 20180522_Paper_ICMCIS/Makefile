FIGURES=$(shell ls figures/*.gnuplot figures/**/*.gnuplot)
FIGURES_PDF=$(FIGURES:%.gnuplot=%.pdf)

pdf: *.tex *.bib clean $(FIGURES_PDF)
	#pdflatex --file-line-error --halt-on-error paper.tex
	latex paper.tex
	bibtex paper.aux
	latex paper.tex
	latex paper.tex
	dvips paper.dvi
	#pdflatex --file-line-error --halt-on-error paper.tex
	#pdflatex --file-line-error --halt-on-error paper.tex
	# To create a PDF with all fonts embedded
	#pdf2ps paper.pdf
	ps2pdf -dCompatibilityLevel=1.4 -dPDFSETTINGS=/prepress paper.ps paper.pdf
	make clean
	
%.pdf: %.gnuplot
	# Make .eps and .tex files
	gnuplot $*.gnuplot
	# Make .pdf file
	ps2pdf -dEPSCrop $*.eps $*.pdf


clean:
	rm -f *.aux *.bbl *.blg *.log *.out *.cb *.cb2 *.ps *~ figures/*.tex figures/*~ *.toc *.synctex.* figures/**/*.tex figures/**/*~
