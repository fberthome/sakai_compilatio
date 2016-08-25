**Compétences requises pour pouvoir réaliser l’installation**
> Il est nécessaire d’avoir une compréhension des technologies Java, Maven et Tomcat. Une connaissance de mysql est souhaitée.
**Environnement technique requis**
> Tomcat version 7_0_65 ou supèrieure, java7, maven. 

**Procédure d’installation**
> Il est supposé que l’utilisateur a installé Sakai_{version}. Les sources de sakai sont présentes dans le répertoire {sakai_src_home}.<br/>
*	Arrêter Tomcat
* Récupérer la branche sakai-10.7 dans votre gestionnaire de source
* récupérer le zip des source (par exemple compilatio-sakai-10.7.zip)
* Copier le zip sous {sakai_src_home} où sakai_src_home est le répertoire parent où sont installées les sources de sakai
* cd {sakai_src_home}
* rm -rf content-review
* cd ..
* Dézipper le fichier compilatio-sakai-10.7.zip
* cd content-review
* mvn clean install sakai:deploy (voir partie déploiement)
* cd ../
* cd assignment
* mvn clean install sakai:deploy (voir partie déploiement)
* cd ../
* cd content-review
* cd contentreview-impl
* mvn clean install sakai:deploy (voir partie déploiement)

**Note**
>Si l’installation du plugin a été effectuée plusieurs fois, il est nécessaire de supprimer les références obsolètes contenu des répertoires suivants : 
* tomcat/work/Catalina/localhost/
* tomcat/components/
* tomcat/shared/lib/

**Déploiement**
> Build et déploiement avec Maven:
> 
GNU/Linux | Windows
--------- | -------
mvn clean install sakai:deploy<br/> -Dmaven.tomcat.home=$CATALINA_HOME | mvn clean install sakai:deploy<br/> -Dmaven.tomcat.home=%CATALINA_HOME%


Si la variable d’environnement CATALINA_HOME n’est pas déclarée, remplacez le path par le répertoire où est installé Tomcat (en général. /opt/tomcat).


**Configuration**
>Il est cénessaire d’éditer le fichier sakai.properties ($CATALINA_HOME/sakai/sakai.properties) et d’y inclure le paramétrage spécifique à Compilatio. Ajouter les lignes suivante, remplissez les champs de manière appropriée:
* compilatio.useContentReview=true
* compilatio.apiURL=http://service.compilatio.net/webservices/CompilatioUserClient.php?
* compilatio.secretKey=[votre Clé Compilatio]

> Configuration de la base de données : Par défaut sakai au lancement génère tous les objets SQL et la base données par défaut utilisée est HSQLDB, pour changer la base de données utilisée se référer à https://confluence.sakaiproject.org/display/DOC/Sakai+10+database+support

**Démarrage et Test**
>Démarrer Tomcat. Le service content review utilise une tâche planlifiée  pour envoyer les soumissions à Compilatio périodiquement. Suivre les instructions suivantes pour configurer cette tâche:

>1. Se connecter à sakai en tant qu’admin
2. Allez  'Job Scheduler' (planificateur de Taches)
3. Cliquer sur 'Jobs'
4. Cliquer sur 'New job'
5. Selectionner 'Process Content Review Queue' dans la liste déroulante et choisir un nom pour le nom de la tâche
6. Cliquer sur 'Post'
7. Cliquer sur 'Triggers(0)' pour le tâche précédemment créée
8. Cliquer sur 'New Trigger'
9. Choisir un nom pour le trigger (déclencheur), et entrer une expression cron. Pour une intervale de 5 minutes (ie. 0 */5 * * * ?)
10. Cliquer sur 'Post'

>Pour vérifier la bonne installation du plugin Compilatio, à la création un devoir, dans la section Service de révision , vous aurez une check box vous permettant la vérification du plagiat par Compilatio.


![Creation Assignment Compilatio](http://ludicolo.compilatio.net/imagecompilatio.jpg)
