package main;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

public class Main {
	
	
  
	public static void main(String[] args) {

        // Cr�ation de la session Spark
        SparkSession sparkSession = SparkSession.builder().appName("IntegrationDonnees").master("local").getOrCreate();
        
        // Chargement des donn�es Openfoodfacts depuis le lien CSV
        Dataset<Row> openFoodFactsData = sparkSession.read()
        		.format("csv")
                .option("header", "true")
                .option("delimiter", "\t")  // Utilisation de la tabulation comme s�parateur
                .option("encoding", "UTF-8") // Utilisation de l'encodage UTF-8
                .load("C:/Users/Alyssa/Desktop/en.openfoodfacts.org.products.csv"); //Url du csv sur mon ordinateur, � changer pour que �a fonctionne
        
        // Informations du d�but (root)
        openFoodFactsData.printSchema();
        // Nombre de lignes
        long rowCount = openFoodFactsData.count();

        // Nombre de colonnes
        int columnCount = openFoodFactsData.columns().length;

        // Afficher le r�sultat (cette �tape permet d'avoir une vue d'ensemble du volume des donn�es)
        System.out.println("Le dataset compte " + rowCount + " lignes et " + columnCount + " variables.");
        
        //On fait un tableau pour les colonnes qu'on veut garder
        String[] columnsToKeep = {"code", "product_name","countries_en","nutriscore_grade", "energy_100g","fat_100g","carbohydrates_100g","proteins_100g","salt_100g"};
        openFoodFactsData = openFoodFactsData.selectExpr(columnsToKeep);
        
        // Premier filtre: On ne veut pas que le nom, le pays, le nutriscore ou le nb de kcal soit null
        openFoodFactsData = openFoodFactsData.filter("product_name is not null")
                .filter("countries_en is not null")
                .filter("nutriscore_grade is not null")
                .filter("energy_100g is not null")
                .filter("fat_100g is not null")
                .filter("carbohydrates_100g is not null")
                .filter("proteins_100g is not null")
                .filter("salt_100g is not null");
        
        //On veut enlever les valeurs qui sont dites "abberantes"
        //Le nutriscore n'�tant pas une valeur tr�s importantes, le "unknown" n'est pas d�rangeant
        // Convertir les colonnes pertinentes en type double pour effectuer des calculs
        for (String column : columnsToKeep) {
            // V�rifier si la colonne est une cha�ne de caract�res (string)
            if (openFoodFactsData.schema().apply(column).dataType().simpleString().equals("string")) {
                // Laisser la colonne inchang�e
                continue;
            }
            // Convertir la colonne en double
            openFoodFactsData = openFoodFactsData.withColumn(column, functions.col(column).cast("double"));
        }
        
        // D�finir les bornes acceptables pour chaque colonne
        double[] minValues = {1,0, 0, 0, 0, 0};  // bornes minimales
        double[] maxValues = {900, 100, 100, 100,100,100};  // bornes maximales
        
     // Filtrer les lignes contenant des valeurs aberrantes (ici pour �nergie, lipides, prot�ines, sucre et sel)
        for (int i = 4; i < columnsToKeep.length; i++) { // Commencer � partir de "energy_100g"
            String column = columnsToKeep[i];
            double minValue = minValues[i-4]; // Indice relatif aux bornes dans le tableau minValues
            double maxValue = maxValues[i-4]; // Indice relatif aux bornes dans le tableau maxValues
            openFoodFactsData = openFoodFactsData.filter(openFoodFactsData.col(column).geq(minValue).and(openFoodFactsData.col(column).leq(maxValue)));
        }
        // Nombre de lignes
        long rowCountAfter = openFoodFactsData.count();

        // Nombre de colonnes
        int columnCountAfter = openFoodFactsData.columns().length;

        // Afficher le r�sultat (cette �tape permet d'avoir une vue d'ensemble du volume des donn�es apr�s le netoyage)
        System.out.println("Apr�s le netoyage, le dataset compte " + rowCountAfter + " lignes et " + columnCountAfter + " variables.");
        
        //Afficher les 20 premi�res lignes du tableau
        openFoodFactsData.show();

        ////////////////////////////////////////////////////////////////////////
        
        // Chargement des donn�es des r�gimes depuis le lien CSV
        Dataset<Row> regimesData = sparkSession.read()
        		.format("csv")
                .option("header", "true")
                .option("delimiter", ",")  // Utilisation de la tabulation comme s�parateur
                .option("encoding", "UTF-8") // Utilisation de l'encodage UTF-8
                .load("C:/Users/Alyssa/Desktop/regimes_nutritionnels.csv"); //Url du csv sur mon ordinateur, � changer pour que �a fonctionne
        
        regimesData.show();
        
        // Chargement des donn�es des r�gimes depuis le lien CSV
        Dataset<Row> utilisateursData = sparkSession.read()
        		.format("csv")
                .option("header", "true")
                .option("delimiter", ",")  // Utilisation de la tabulation comme s�parateur
                .option("encoding", "UTF-8") // Utilisation de l'encodage UTF-8
                .load("C:/Users/Alyssa/Desktop/utilisateurs_regimes.csv"); //Url du csv sur mon ordinateur, � changer pour que �a fonctionne
        
        utilisateursData.show();
        
        

	}

}
