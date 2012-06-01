/* Getopt for Microsoft C  
This code is a modification of the Free Software Foundation, Inc. 
Getopt library for parsing command line argument the purpose was
to provide a Microsoft Visual C friendly derivative. This code
provides functionality for both Unicode and Multibyte builds.

Date: 02/03/2011 - Ludvik Jerabek - Initial Release
Version: 1.0
Comment: Supports getopt, getopt_long, and getopt_long_only
and POSIXLY_CORRECT environment flag
License: LGPL

Revisions:

02/03/2011 - Ludvik Jerabek - Initial Release
02/20/2011 - Ludvik Jerabek - Fixed compiler warnings at Level 4

**DISCLAIMER**
THIS MATERIAL IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING, BUT Not LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE, OR NON-INFRINGEMENT. SOME JURISDICTIONS DO NOT ALLOW THE
EXCLUSION OF IMPLIED WARRANTIES, SO THE ABOVE EXCLUSION MAY NOT
APPLY TO YOU. IN NO EVENT WILL I BE LIABLE TO ANY PARTY FOR ANY
DIRECT, INDIRECT, SPECIAL OR OTHER CONSEQUENTIAL DAMAGES FOR ANY
USE OF THIS MATERIAL INCLUDING, WITHOUT LIMITATION, ANY LOST
PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA ON
YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE, EVEN If WE ARE
EXPRESSLY ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. 
*/
#ifndef __GETOPT_H_
#define __GETOPT_H_

#include <tchar.h>

#define ARG_NULL 0 /*Argument Null*/
#define ARG_NONE 0 /*Argument Switch Only*/
#define ARG_REQ 1  /*Argument Required*/
#define ARG_OPT 2  /*Argument Optional*/

// Change behavior for C\C++
#ifdef __cplusplus
#define _BEGIN_EXTERN_C extern "C" {
#define _END_EXTERN_C }
#define _GETOPT_THROW throw()
#else
#define _BEGIN_EXTERN_C
#define _END_EXTERN_C
#define _GETOPT_THROW
#endif

_BEGIN_EXTERN_C
extern TCHAR *optarg;
extern int optind;
extern int opterr;
extern int optopt;
struct option
{
	const TCHAR* name;
	int has_arg;
	int *flag;
	TCHAR val;
};
extern int getopt(int argc, TCHAR *const *argv, const TCHAR *optstring) _GETOPT_THROW;
extern int getopt_long(int ___argc, TCHAR *const *___argv, const TCHAR *__shortopts, const struct option *__longopts, int *__longind) _GETOPT_THROW;
extern int getopt_long_only(int ___argc, TCHAR *const *___argv, const TCHAR *__shortopts, const struct option *__longopts, int *__longind) _GETOPT_THROW;
_END_EXTERN_C

// Undefine so the macros are not included
#undef _BEGIN_EXTERN_C
#undef _END_EXTERN_C
#undef _GETOPT_THROW

#endif  // __GETOPT_H_
