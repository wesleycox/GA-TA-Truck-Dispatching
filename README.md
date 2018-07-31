# GA-TA Truck Dispatching

Email wesley.cox@uwa.edu.au for any queries.
#
Instructions:

The code can be compiled by running the compile batch file.

This requires Apache Ant; for installation instructions see:

http://ant.apache.org/

Additionally, some classes use the lp_solve software library for mixed integer linear programming (MILP), which is under the GNU LGPL license. 

The Java .jar library for lp_solve has been included in this distribution. Include it the classpath when required. Further installation is required to make use of it however; for installation instructions and further information see:

lpsolve.sourceforge.net/5.5

Example main files are provided: Main.java and MainN.java, for simple and complex network problems respectively.

Main can be run as:

java -cp .;classes;lib/lpsolve55j.jar Main filename numSamples runtime solIndex...

	filename	an valid input file path, e.g. input/problem1-1.in

	numSamples	the integer number of simulations to run per solution, e.g. 50

	runtime		the real-valued shift length per simulation, e.g. 500

	solIndex	a valid solution index -- 0 for the GA, 1 for MTCT, 2 for MTWT, 3 for MTST, 4 for MSWT, 5 for DISPATCH

MainN is run similarly.
#
Input:

Input files used in the paper are provided. 

Input files come in two formats. The first is for simple road networks and is loaded using the MineParameters class for use with the MineSimulatorNarrow class; and the second is for complex road networks and is loaded using the MineParametersN class for use with the MineSimulatorNarrowMCNWTL class.
#
The first format is described here:

First line:

T _NT_

where _NT_ is the number of trucks.

Next two lines:

C 1

_EM ESD_

where _EM_ is the mean emptying time at the crusher, and _ESD_ is the standard deviation.

Next line:

S _NS_

where _NS_ is the number of shovels.

_NS_ lines of:

_TTM TTSD FM FSD_

where for the nth line, _TTM_ is the mean travel time to the nth shovel, _TTSD_ is the standard deviation, _FM_ is the mean filling time at the nth shovel, and _FSD_ is the standard deviation.

Any deviation from this format will result in error.
#
The second format is described here:

First line:

T _NT FS_

where _NT_ is the number of trucks, and _FS_ is the travel time multiplier for full trucks.

Second line:

C _NC_

where _NC_ the number of crushers.

Next _NC_ lines:

_EM ESD_

where for the nth line, _EM_ is the mean emptying time at the nth crusher, and _ESD_ is the standard deviation.

Next line:

S _NS_

where _NS_ is the number of shovels.

_NS_ lines of:

_FM FSD_

where for the nth line, _FM_ is the mean filling time at the nth shovel, and _FSD_ is the standard deviation.

Next line:

R _NR_ N _NN_

where _NR_ is the number of roads, and _NN_ is the number of nodes in the road network (excluding crushers and shovels).

_NR_ lines of:

_n1 i1 n2 i2 TTM TTSD rt_

where the nth line describes the nth road; _n1_ and _n2_ are node types (_c_ for crusher, _s_ for shovel, _n_ for node), _i1_ and _i2_ are the corresponding indexes for the nodes, _TTM_ is the mean travelling time along the road, _TTSD_ is the standard deviation, and _rt_ is the road type (_t_ for two-lane, _o_ for one-lane).

Any deviation from this format will result in error.
#
Notes:

Please ignore the weird class names and package structure. The original meanings are lost in time, and this was a small portion of a larger project with the package structure retained.
#
Copyright (C) 2017,  Wesley Cox, Lyndon While, Tim French, Mark Reynolds

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
