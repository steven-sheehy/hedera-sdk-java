# for file in sdk/build/docs/javadoc/com/hedera/hashgraph/sdk/*.html
for file in $(find -L sdk/build/docs/javadoc/com/hedera/hashgraph/sdk/ -regex ".*\/[A-Z].*.html")
do
    FILE_NAME=$(echo $file | sed -e 's|\(\S\+\/\)\+\(\w\+\).html|\2.txt|g')
    touch api/$FILE_NAME
    html2text --no-wrap-links $file | \
        perl -pe "s|\[(.*?)]\(.*?\)|\1|g" | \
        sed -n "/^\(@\S\+\s\)*public/p" | \
        sed -e "s#^\s\+##g" | \
        sed -e "s#^\s\+##g" | \
        sed -e "s/$(echo -ne '\u200b')//g" | \
        sort > api/$FILE_NAME
done
