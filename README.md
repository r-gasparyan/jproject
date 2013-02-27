jproject
========
Project Description
The scenario for this system is a (real!) remote encampment in northern Scandinavia. 
The encampment can only be reached by helicopter, and is serviced by three private 
helicopter companies providing access to the nearest town. Visitors and residents can 
book passage in or out of the encampment through any helicopter company. The number of 
seats on a helicopter flight is very limited and the companies are collectively responsible 
for satisfying demand, and must load-share as necessary.

You have to design, build, test and demonstrate a distributed booking system for the 
helicopter companies.

The core idea of the project is to choose and implement an appropriate organization of 
the system and suitable algorithms for ticket booking and cancellation. The system should 
not impose arbitrary restrictions on the ability of potential clients to make or cancel 
bookings. It must when a number of helicopters are in flight---in particular, it must not 
be confined to working only when one helicopter is in transit.

The prototype implementation should use only sockets for communication between the parts of 
the system and should be written in Java.

Your system should be built in a way that conforms to good distributed system practice. 
Your design may ignore security concerns. You may assume that nodes and disks do not fail 
but you should allow for the possibility of communication failure and for individual node 
unavailability. You need only provide a rudimentary user interface. In designing the system 
you should consider that such a system might have to scale to support different deployments.
