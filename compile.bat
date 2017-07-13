mkdir classes
call ant clean
call ant compile
javac -cp .;classes Main.java MainN.java
pause