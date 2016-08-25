**Comp�tences requises pour pouvoir r�aliser l�installation**
> Il est n�cessaire d�avoir une compr�hension des technologies Java, Maven et Tomcat. Une connaissance de mysql est souhait�e.
**Environnement technique requis**
> Tomcat version 7_0_65 ou sup�rieure, java7, maven. 

**Proc�dure d�installation**
> Il est suppos� que l�utilisateur a install� Sakai_{version}. Les sources de sakai sont pr�sentes dans le r�pertoire {sakai_src_home}.
>	Arr�ter Tomcat
> R�cup�rer la branche sakai-10.7 dans votre gestionnaire de source
> r�cup�rer le  zip des source (par exemple compilatio-sakai-10.7.zip)
> Copier le zip sous {sakai_src_home} o� sakai_src_home est le r�pertoire parent o� sont install�es les sources de sakai
>
>cd {sakai_src_home}
>rm -rf content-review
>cd ..
>D�zipper le fichier compilation-10.7.zip
>cd content-review
>mvn clean install sakai:deploy (voir partie d�ploiement)
>cd ../
>cd assignment
>mvn clean install sakai:deploy (voir partie d�ploiement)
>cd ../
>cd content-review
>cd contentreview-impl
>mvn clean install sakai:deploy (voir partie d�ploiement)

**Note**
>Si l�installation du plugin a �t� effectu�e plusieurs fois, il est n�cessaire de supprimer les r�f�rences obsol�tes contenu des r�pertoires suivants : 
* tomcat/work/Catalina/localhost/
* tomcat/components/
* tomcat/shared/lib/

**D�ploiement**
> Build et d�ploiement avec Maven:
> 
GNU/Linux | Windows
--------- | -------
mvn clean install sakai:deploy<br/> -Dmaven.tomcat.home=$CATALINA_HOME | mvn clean install sakai:deploy<br/> -Dmaven.tomcat.home=%CATALINA_HOME%


Si la variable d�environnement CATALINA_HOME n�est pas d�clar�e, remplacez le path par le r�pertoire o� est install� Tomcat (en g�n�ral. /opt/tomcat).


**Configuration**
>Il est c�nessaire d��diter le fichier sakai.properties ($CATALINA_HOME/sakai/sakai.properties) et d�y inclure le param�trage sp�cifique � Compilatio. Ajouter les lignes suivante, remplissez les champs de mani�re appropri�e:
* compilatio.useContentReview=true
* compilatio.apiURL=http://service.compilatio.net/webservices/CompilatioUserClient.php?
* compilatio.secretKey=[votre Cl� Compilatio]

> Configuration de la base de donn�es : Par d�faut sakai au lancement g�n�re tous les objets SQL et la base donn�es par d�faut utilis�e est HSQLDB, pour changer la base de donn�es utilis�e se r�f�rer � https://confluence.sakaiproject.org/display/DOC/Sakai+10+database+support

**D�marrage et Test**
>D�marrer Tomcat. Le service content review utilise une t�che planlifi�e  pour envoyer les soumissions � Compilatio p�riodiquement. Suivre les instructions suivantes pour configurer cette t�che:

>1. Se connecter � sakai en tant qu�admin
2. Allez  'Job Scheduler' (planificateur de Taches)
3. Cliquer sur 'Jobs'
4. Cliquer sur 'New job'
5. Selectionner 'Process Content Review Queue' dans la liste d�roulante et choisir un nom pour le nom de la t�che
6. Cliquer sur 'Post'
7. Cliquer sur 'Triggers(0)' pour le t�che pr�c�demment cr��e
8. Cliquer sur 'New Trigger'
9. Choisir un nom pour le trigger (d�clencheur), et entrer une expression cron. Pour une intervale de 5 minutes (ie. 0 */5 * * * ?)
10. Cliquer sur 'Post'

>Pour v�rifier la bonne installation du plugin Compilatio, � la cr�ation un devoir, dans la section Service de r�vision , vous aurez une check box vous permettant la v�rification du plagiat par Compilatio.


![Creation Assignment Compilatio](http://ludicolo.compilatio.net/imagecompilatio.jpg)