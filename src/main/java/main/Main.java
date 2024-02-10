package main;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import static org.apache.spark.sql.functions.*;

public class Main {
	private static SparkSession sparkSession;
	
  
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
        
            // Supprimer les lignes vides
            regimesData = regimesData.na().drop();

                 // Supprimer les lignes avec des valeurs manquantes ou mal formatées
                    regimesData = regimesData
                    .filter(col("regime_alimentaire").isNotNull())
                    .filter(col("max_glucides_g").isNotNull())
                    .filter(col("max_proteines_g").isNotNull())
                    .filter(col("max_lipides_g").isNotNull())
                    .filter(col("max_calories").isNotNull());
            
                     // Corriger le format des valeurs numériques
                    regimesData = regimesData
                    .withColumn("max_calories", regexp_replace(col("max_calories"), ";", "").cast("int"))
                    .withColumn("max_glucides_g", regexp_replace(col("max_glucides_g"), ";", "").cast("int"))
                    .withColumn("max_proteines_g", regexp_replace(col("max_proteines_g"), ";", "").cast("int"))
                    .withColumn("max_lipides_g", regexp_replace(col("max_lipides_g"), ";", "").cast("int"));
    
            regimesData.show();
        
        // Chargement des donn�es des r�gimes depuis le lien CSV
        Dataset<Row> utilisateursData = sparkSession.read()
        		.format("csv")
                .option("header", "true")
                .option("delimiter", ",")  // Utilisation de la tabulation comme s�parateur
                .option("encoding", "UTF-8") // Utilisation de l'encodage UTF-8
                .load("C:/Users/Alyssa/Desktop/utilisateurs_regimes.csv"); //Url du csv sur mon ordinateur, � changer pour que �a fonctionne

                 // Supprimer les lignes vides
        utilisateursData = utilisateursData.na().drop();
        
        utilisateursData.show();

        // Intégration des informations des utilisateurs avec les seuils des régimes alimentaires

        Dataset<Row> menuPersonnalise = utilisateursData
        	    .join(regimesData, utilisateursData.col("regime_alimentaire").equalTo(regimesData.col("regime")))
        	    .select("utilisateur_id", "regime_alimentaire", "max_glucides_g", "max_proteines_g", "max_lipides_g", "max_calories");
        
        menuPersonnalise.show();  

        //Générer aléatoirement un menu sur une semaine
        Dataset<Row> menuHebdomadaire = genererMenuHebdomadaire(openFoodFactsData, menuPersonnalise);
        
        //Affichage de menu pour la semaine
        System.out.println("Voici le menu de la semaine :");
        menuHebdomadaire.show();

	}
    // Fonction pour générer aléatoirement un menu hebdomadaire équilibré
	
    private static Dataset<Row> genererMenuHebdomadaire(Dataset<Row> openFoodFactsData, Dataset<Row> menuPersonnalise) {
    	// Initialisation e DataFrame pour le menu hebdomadaire
    	 Dataset<Row> menuHebdomadaire = openFoodFactsData.filter(col("energy_100g").isNotNull())
    	            .filter(col("fat_100g").isNotNull())
    	            .filter(col("carbohydrates_100g").isNotNull())
                    .filter(col("proteins_100g").isNotNull())
    	            .filter(col("salt_100g").isNotNull());
    	 
            // Répéter pour chaque jour de la semaine
            for (int jour = 1; jour <= 7; jour++) {
                // Sélectionner aléatoirement des produits pour chaque jour
                Dataset<Row> menuJour = menuHebdomadaire.sample(false, 0.1).limit(7).withColumn("jour", lit(jour));

                //On affiche le menu de chaque jour: 

                System.out.println("Voici le menu du jour :");
                menuJour.show();
                // On ajoute le menu du jour au menu hebdomadaire
                
                if (jour == 1) {
                    menuHebdomadaire = menuJour;
                } else {
                    menuHebdomadaire = menuHebdomadaire.union(menuJour);
                }
            }
            //stocker le menu dans le DWH : //besoin d'installer HADOOP pour cette partie
            String menuHebdomadairePath = "C:/epsi/data/menu_hebdomadaire1";
            menuHebdomadaire.write()
                    .format("csv")
                    .option("header", "true")
                    .option("delimiter", ",")
                    .mode("overwrite")
                    .save(menuHebdomadairePath);
            
            // Création d'une table dans le DWH pour stocker le menu hebdomadaire
            menuHebdomadaire.createOrReplaceTempView("table_menu_hebdomadaire");
            
            // Création d'une table pour lier le menu hebdomadaire à l'utilisateur final
            String sqlCreateLinkTable = "CREATE TABLE IF NOT EXISTS table_lien_utilisateur_menu (" +
                    "utilisateur_id INT, " +
                    "menu_id INT, " +
                    "jour INT)";
            sparkSession.sql(sqlCreateLinkTable);
            
             // On ajoute les informations de liaison dans la table du DWH
            String sqlInsertLinkInfo = "INSERT INTO table_lien_utilisateur_menu VALUES (utilisateur_id, menu_id, jour)";
            sparkSession.sql(sqlInsertLinkInfo);

            // Affichage du menu hebdomadaire (c'est le seul résultat observable par l'utilisateur final)
            System.out.println("Voici le menu de la semaine :");
            menuHebdomadaire.show();

        return menuHebdomadaire;
    }
}
