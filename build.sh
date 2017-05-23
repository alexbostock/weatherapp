#!\bin\sh
# Packages the app for submission

mkdir weatherapp

# TODO - make jar

cp changes/changes.pdf weatherapp
cp -r com weatherapp
mkdir weatherapp/data
cp data/cityList.txt weatherapp
cp -r InteractionDesign weatherapp
cp *.txt weatherapp
cp run.* weatherapp

zip -r Task3_Group03 weatherapp

rm weatherapp

echo ""
echo "Remember to:"
echo "	Update readme"
echo "	Check run.bat and run.sh"
echo "	Update docs"
echo "	Include Changes_GroupXY.pdf"

