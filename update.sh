#!/bin/bash

function get_name {
    local x=$1

    x=${x//)/}
    x=${x// (/__}
    x=${x// /_}

    x=${x/be-official/Belgian}
    x=${x/German_de_DE/German}
    x=${x/German_de_AT/Austrian}
    x=${x/German_de_CH/Swiss}
    x=${x/Ukrainian_uk_UA/Ukrainian}
    x=${x/Vietnamese_vi_VN/Vietnamese}
    x=${x/Slovak_sk_SK/Slovak}

    x=${x/Armenian__Western/Armenian}
    x=${x/Armenian__Eastern/Armenian_DELETE}
    x=${x/Romanian__Modern/Romanian}
    x=${x/Romanian__Ante1993/Romanian_DELETE}
    x=${x/Portuguese__European_-_Before_OA_1990/Portuguese_DELETE}
    x=${x/Russian-English_Bilingual/Russian_DELETE}
    x=${x/Serbian__Cyrillic/Serbian}
    x=${x/Serbian__Latin/Serbian_DELETE}

    x=${x/Turkish/Hungarian_DELETE} # not working
    x=${x/Turkish/Turkish_DELETE} # not working
    x=${x/Basque/Basque_DELETE} # not working

   echo "$x"
}

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
rm -rf tmp && mkdir -p tmp

curl -L https://github.com/titoBouzout/Dictionaries/archive/master.zip > tmp/dictionaries.zip
unzip -q tmp/dictionaries.zip -d tmp

mv tmp/*-master/*.aff tmp && mv tmp/*-master/*.dic tmp
rm -rf tmp/*-master && rm -rf tmp/dictionaries.zip

cd tmp && {
    for old in *;do {
        new=$(get_name "$old");
        [[ "$old" != "$new" ]] && mv "$old" "$new";
    } done;

    rm -rf la.*
    rm -rf *_DELETE.*
}
cd ..

rm -rf resources/hunspell/*
mv tmp/*.dic resources/hunspell/
mv tmp/*.aff resources/hunspell/

// TODO: sed unicode

sed -i "s/SET UTF-8/SET UTF-8\nFLAG UTF-8/g" resources/hunspell/*.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Spanish.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Russian.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Persian.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Mongolian.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Korean.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Indonesia.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Icelandic.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Galego.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/French.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Danish.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Dutch.aff
sed -i "s/nFLAG UTF-8//" resources/hunspell/Armenian.aff

echo "private static String[] dictionaries = {"

cd resources/hunspell/ && for n in *.dic; do {
    x=$(printf '%s\n' "$n")
    x=${x//__/ - }
    x=${x//_/ }
    x=${x//\.dic/}

    echo "    \"$x\","
} done

echo "};"

cd ../..

rm -rf tmp
