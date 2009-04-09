set INSPATH=%~dp0
cd "%INSPATH%"
copy %1 "%INSPATH%Megaupload.gif"

symgen -i Megaupload.gif -p mu.filter -o megaupload
symseek_uf -me 2 -i megaupload.png -o Megaupload.txt -olf 0.8 -e 2 -db mu0903.db -l 4
del megaupload.png
