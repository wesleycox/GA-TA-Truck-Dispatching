# GA-TA Truck Dispatching

Email wesley.cox@uwa.edu.au for any queries.

Instructions:

The code can be compiled by running the compile batch file.

This requires Apache Ant; for installation instructions see:
<http://ant.apache.org/>

Additionally, some classes use the lp_solve software for mixed integer linear programming (MILP). The Java .jar library for lp_solve has been included in this distribution. Include it the classpath when required. Further installation is required to make use of it however; for installation instructions see:
<lpsolve.sourceforge.net/5.5>

Input:
Input files used in the paper are provided. 
Input files come in two formats. The first is for simple road networks and is loaded using the MineParameters class for use with the MineSimulatorNarrow class; and the second is for complex road networks and is loaded using the MineParametersN class for use with the MineSimulatorNarrowMCNWTL class.

The first format is described here:
#
First line:
T NT
where NT is the number of trucks.
#
Next two lines:
C 1
EM ESD
where EM is the mean emptying time at the crusher, and ESD is the standard deviation.
#
Next line:
S NS
where NS is the number of shovels.
#
NS lines of:
FM FSD
where for the nth line, FM is the mean filling time at the nth shovel, and FSD is the standard deviation.
#
Any deviation from this format will result in error.

Notes:
Please ignore the weird class names and package structure. The original meanings are lost in time, and this was a small portion of a larger project with the package structure retained.

Copyright (C) 2017,  Wesley Cox, Lyndon While, Tim French, Mark Reynolds

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.