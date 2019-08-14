Custom Tools Plugin for Jenkins
==================

A generic tool installer. You define how tools get installed, and the plugin will automatically install them when needed. 

## Overview 

A job will often require a tool not already installed on your Jenkins agents. 
In large environments, this often results in waiting on an administrator. 
The goal of this plugin is to let users manage their own tools, without requiring this administrator involvement.

Using this plugin, you can define a script (or just a URL) for installing a tool using standard Jenkins [Tool Installers](https://jenkins.io/doc/developer/extensions/jenkins-core/#toolinstaller). 
Plugins like [Extra Tool Installers](https://plugins.jenkins.io/extra-tool-installers) can be also used in this plugin. 
You then define which jobs require the tool, and the plugin installs them as needed before the build runs.

## Usage

### Defining tools

Let's say that you have a build that needs NodeJS. 
It is possible to use a [NodeJS Plugin](https://plugins.jenkins.io/nodejs) for it, but we will use it as an example.

1. Go to the _Manage Jenkins_ > _Global Tool Configuration_ page and to find the _Custom Tool_ section there.
2. Add a new _NodeJS_ Tool
3. Configure the tool installer. E.g. you can use a script as in the example below
4. Configure _Exported paths_. The field lets you specify multiple directory patterns which will be added to the $PATH so that your build needn't know where the tool is actually installed

![Tool Configuration](/docs/images/configure_tool.png)

### Using tools in Freestyle projects

For Freestyle projects you need to add the tool requirement to your job's _Build Environment_ (aka "build wrappers").

![Freestyle Project. Tool Build Wrapper](/docs/images/buildWrapper.png)

Then, you can just use the tool in your job, without having to know where it's installed. 
The plugin will install it as needed before your job runs, and make sure the `$PATH` is setup correctly:

![Freestyle Project. Tool Installation log](/docs/images/installLog.png)

## Advanced use-cases

### Tool versioning

