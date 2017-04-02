# Setup

* Import sourcelookup-pde demo Eclipse app as existing maven project

# Debug decentxml.app as Advanced Eclipse Application

* Set breakpoint at decentxml.app.Application #16

Expected: debugger properly positioned at Application sources
Expected: Display view provides codeassist for 'do...' doc, shows value
Expected: Variables view, open declared type of doc opens decentxml source
Expected: step into doc.toString() works

# Debug decentxml.app.test as Advanced JUnit Plug-In Test

* Set breakpoint at decentxml.app.test.ApplicationTest #12

Expected: debugger stops properly positioned at ApplicationTest sources
Expected: step into Fragement.newDocument() works
Expected: step into Application.start(...) works
Expected: step into decentxml code works 

# Tycho wrapper of a workspace project

* Import decentxml from https://bitbucket.org/digulla/decentxml

    hg clone -b r1.4 https://bitbucket.org/digulla/decentxml

Expected: decentxml.wrapper dependency switched to decentxml workspace project

* Set breakpoint at Document.toString, debug decentxml.app

Expected: debugger stops properly positioned at decentxml sources 
