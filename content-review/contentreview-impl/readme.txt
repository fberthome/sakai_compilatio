Compilatio Content Review Service
===============================

.. author Fabien Berthomé

Installation
------------


This module can be installed with the standard Sakai maven idiom.
Additionally, it can be included with a Sakai distribution as a top
level module as is typical with other 3rd party modules and tools.

The standard build command is the same:

::

  mvn clean install sakai:deploy

Post Install Configuration
--------------------------

After compiling and installing the code you need to add some
sakai.properties with your account information, and then set up
some quartz jobs that submit and fetch papers from the Compilatio
web service.

Sakai Properties
~~~~~~~~~~~~~~~~

Sakai Compilatio Content Review configurations typically fall in to 
2 categories.  The original iteration of development had a single 
instructor account that controlled all the classes for the integration.
More recent versions have allowed using an option that enables
each instructor in Sakai to have a fully provisioned account as
well which makes some tighter integration options possible.

We'll cover the newer setup first.  Assuming you have a Compilatio
Account/Contract already, you'll need to log in to compilatio.com
and create a new subaccount on your main account. You'll then need
to configure the integration to use the Open API.  Very soon there
will be an option there for Sakai (along with the other Course Management
Systems), but until then you'll need to email David Wu at Compilatio
and request that this account be "Source 9 Enabled".  His email is
davidw At iparadigms Dot com

You'll need the shared key and Account ID for the properties.

The properties then are as follows:

:: 

  compilatio.apiURL=https://www.compilatio.com/api.asp?
  compilatio.secretKey=mysecret # This is the secret you set online.




Usefull logging settings for this project include:

DEBUG.org.sakaiproject.contentreview.impl.compilatio
DEBUG.org.sakaiproject.compilatio.util.CompilatioAPIUtil.apicalltrace

Quartz (Cron) Jobs
~~~~~~~~~~~~~~~~~~

There are 2 mandatory quartz jobs that need to be set up, and a third
Please see either the readme.txt or readme.html in the docs folder.
