/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.experiment.math.optimization.lbfgs;

import java.io.Serial;

/**
 * <p> This class contains code for the limited-memory Broyden-Fletcher-Goldfarb-Shanno
 * (LBFGS) algorithm for large-scale multidimensional unconstrained minimization problems.
 * This file is a translation of Fortran code written by Jorge Nocedal.
 * The only modification to the algorithm is the addition of a cache to
 * store the result of the most recent line search. See <tt>solution_cache</tt> below.
 * <p>
 * LBFGS is distributed as part of the RISO project. Following is a message from Jorge Nocedal:
 * <pre>
 *   From: Jorge Nocedal [mailto:nocedal@dario.ece.nwu.edu]
 *   Sent: Friday, August 17, 2001 9:09 AM
 *   To: Robert Dodier
 *   Subject: Re: Commercial licensing terms for LBFGS?
 *
 *   Robert:
 *   The code L-BFGS (for unconstrained problems) is in the public domain.
 *   It can be used in any commercial application.
 *
 *   The code L-BFGS-B (for bound constrained problems) belongs to
 *   ACM. You need to contact them for a commercial license. It is
 *   algorithm 778.
 *
 *   Jorge
 * </pre>
 * <p>
 * <p> This code is derived from the Fortran program <code>lbfgs_cimpl.f</code>.
 * The Java translation was effected mostly mechanically, with some
 * manual clean-up; in particular, array indices start at 0 instead of 1.
 * Most of the comments from the Fortran code have been pasted in here
 * as well.</p>
 * <p>
 * <p> Here's some information on the original LBFGS Fortran source code,
 * available at <a href="http://www.netlib.org/opt/lbfgs_um.shar">
 * http://www.netlib.org/opt/lbfgs_um.shar</a>. This info is taken
 * verbatim from the Netlib blurb on the Fortran source.</p>
 * <p>
 * <pre>
 * 	file    opt/lbfgs_um.shar
 * 	for     unconstrained optimization problems
 * 	alg     limited memory BFGS method
 * 	by      J. Nocedal
 * 	contact nocedal@eecs.nwu.edu
 * 	ref     D. c. Liu and J. Nocedal, ``On the limited memory BFGS method for
 * 	,       large scale optimization methods'' Mathematical Programming 45
 * 	,       (1989), pp. 503-528.
 * 	,       (Postscript file of this paper is available via anonymous ftp
 * 	,       to eecs.nwu.edu in the directory pub/lbfgs_cimpl/lbfgs_um.)
 * </pre>
 *
 * @author Jorge Nocedal: original Fortran version, including comments
 * (July 1990). Robert Dodier: Java translation, August 1997.
 */

public class LBFGS {
    private static LBFGS instance;

    private LBFGS() {
    }

    public static synchronized LBFGS getInstance() {
        if (instance == null)
            instance = new LBFGS();
        return instance;
    }


    /**
     * Controls the accuracy of the line search <code>mcsrch</code>. If the
     * function and gradient evaluations are inexpensive with respect
     * to the cost of the iteration (which is sometimes the case when
     * solving very large problems) it may be advantageous to set <code>gtol</code>
     * to a small value. A typical small value is 0.1.  Restriction:
     * <code>gtol</code> should be greater than 1e-4.
     */

    public double gtol = 0.9;
    /**
     * Specify lower bound for the step in the line search.
     * The default value is 1e-20. This value need not be modified unless
     * the exponent is too large for the machine being used, or unless
     * the problem is extremely badly scaled (in which case the exponent
     * should be increased).
     */

    public final double stpmin = 1e-20;
    /**
     * Specify upper bound for the step in the line search.
     * The default value is 1e20. This value need not be modified unless
     * the exponent is too large for the machine being used, or unless
     * the problem is extremely badly scaled (in which case the exponent
     * should be increased).
     */

    public final double stpmax = 1e20;
    /**
     * The solution vector as it was at the end of the most recently
     * completed line search. This will usually be different from the
     * return value of the parameter <tt>x</tt> of <tt>lbfgs_cimpl</tt>, which
     * is modified by line-search steps. A caller which wants to stop the
     * optimization iterations before <tt>LBFGS.lbfgs_cimpl</tt> automatically stops
     * (by reaching a very small gradient) should copy this vector instead
     * of using <tt>x</tt>. When <tt>LBFGS.lbfgs_cimpl</tt> automatically stops,
     * then <tt>x</tt> and <tt>solution_cache</tt> are the same.
     */
    public double[] solution_cache = null;
    private double gnorm = 0;
    private double stp1 = 0;
    private double ftol = 0;
    private final double[] stp = new double[1];
    private double ys = 0;
    private double yy = 0;
    private double sq = 0;
    private double yr = 0;
    private double beta = 0;
    private double xnorm = 0;
    private int iter = 0;
    private int nfun = 0;
    private int point = 0;
    private int ispt = 0;
    private int iypt = 0;
    private int maxfev = 0;
    private final int[] info = new int[1];
    private int bound = 0;
    private int npt = 0;
    private int cp = 0;
    private int i = 0;
    private final int[] nfev = new int[1];
    private int inmc = 0;
    private int iycn = 0;
    private int iscn = 0;
    private boolean finish = false;
    private double[] w = null;

    /**
     * This method returns the total number of evaluations of the objective
     * function since the last time LBFGS was restarted. The total number of function
     * evaluations increases by the number of evaluations required for the
     * line search; the total is only increased after a successful line search.
     */
    public int nfevaluations() {
        return nfun;
    }

    /**
     * This subroutine solves the unconstrained minimization problem
     * <pre>
     *     min f(x),    x = (x1,x2,...,x_n),
     * </pre>
     * using the limited-memory BFGS method. The routine is especially
     * effective on problems involving a large number of variables. In
     * a typical iteration of this method an approximation <code>Hk</code> to the
     * inverse of the Hessian is obtained by applying <code>m</code> BFGS updates to
     * a diagonal matrix <code>Hk0</code>, using information from the previous M steps.
     * The user specifies the number <code>m</code>, which determines the amount of
     * storage required by the routine. The user may also provide the
     * diagonal matrices <code>Hk0</code> if not satisfied with the default choice.
     * The algorithm is described in "On the limited memory BFGS method
     * for large scale optimization", by D. Liu and J. Nocedal,
     * Mathematical Programming B 45 (1989) 503-528.
     * <p>
     * The user is required to calculate the function value <code>f</code> and its
     * gradient <code>g</code>. In order to allow the user complete control over
     * these computations, reverse  communication is used. The routine
     * must be called repeatedly under the control of the parameter
     * <code>iflag</code>.
     * <p>
     * The steplength is determined at each iteration by means of the
     * line search routine <code>mcsrch</code>, which is a slight modification of
     * the routine <code>CSRCH</code> written by More' and Thuente.
     * <p>
     * The only variables that are machine-dependent are <code>xtol</code>,
     * <code>stpmin</code> and <code>stpmax</code>.
     * <p>
     * Progress messages and non-fatal error messages are printed to <code>System.err</code>.
     * Fatal errors cause exception to be thrown, as listed below.
     *
     * @param n      The number of variables in the minimization problem.
     *               Restriction: <code>n &gt; 0</code>.
     * @param m      The number of corrections used in the BFGS update.
     *               Values of <code>m</code> less than 3 are not recommended;
     *               large values of <code>m</code> will result in excessive
     *               computing time. <code>3 &lt;= m &lt;= 7</code> is recommended.
     *               Restriction: <code>m &gt; 0</code>.
     * @param x      On initial entry this must be set by the user to the values
     *               of the initial estimate of the solution vector. On exit with
     *               <code>iflag = 0</code>, it contains the values of the variables
     *               at the best point found (usually a solution).
     * @param f      Before initial entry and on a re-entry with <code>iflag = 1</code>,
     *               it must be set by the user to contain the value of the function
     *               <code>f</code> at the point <code>x</code>.
     * @param g      Before initial entry and on a re-entry with <code>iflag = 1</code>,
     *               it must be set by the user to contain the components of the
     *               gradient <code>g</code> at the point <code>x</code>.
     * @param diagco Set this to <code>true</code> if the user  wishes to
     *               provide the diagonal matrix <code>Hk0</code> at each iteration.
     *               Otherwise it should be set to <code>false</code> in which case
     *               <code>lbfgs_cimpl</code> will use a default value described below. If
     *               <code>diagco</code> is set to <code>true</code> the routine will
     *               return at each iteration of the algorithm with <code>iflag = 2</code>,
     *               and the diagonal matrix <code>Hk0</code> must be provided in
     *               the array <code>diag</code>.
     * @param diag   If <code>diagco = true</code>, then on initial entry or on
     *               re-entry with <code>iflag = 2</code>, <code>diag</code>
     *               must be set by the user to contain the values of the
     *               diagonal matrix <code>Hk0</code>. Restriction: all elements of
     *               <code>diag</code> must be positive.
     * @param iprint Specifies output generated by <code>lbfgs_cimpl</code>.
     *               <code>iprint[0]</code> specifies the frequency of the output:
     *               <ul>
     *               <li> <code>iprint[0] &lt; 0</code>: no output is generated,
     *               <li> <code>iprint[0] = 0</code>: output only at first and last iteration,
     *               <li> <code>iprint[0] &gt; 0</code>: output every <code>iprint[0]</code> iterations.
     *               </ul>
     *               <p>
     *               <code>iprint[1]</code> specifies the type of output generated:
     *               <ul>
     *               <li> <code>iprint[1] = 0</code>: iteration count, number of function
     *               evaluations, function value, norm of the gradient, and steplength,
     *               <li> <code>iprint[1] = 1</code>: same as <code>iprint[1]=0</code>, plus vector of
     *               variables and  gradient vector at the initial point,
     *               <li> <code>iprint[1] = 2</code>: same as <code>iprint[1]=1</code>, plus vector of
     *               variables,
     *               <li> <code>iprint[1] = 3</code>: same as <code>iprint[1]=2</code>, plus gradient vector.
     *               </ul>
     * @param eps    Determines the accuracy with which the solution
     *               is to be found. The subroutine terminates when
     *               <pre>
     *                                                      ||G|| &lt; EPS max(1,||X||),
     *                                           		</pre>
     *               where <code>||.||</code> denotes the Euclidean norm.
     * @param xtol   An estimate of the machine precision (e.g. 10e-16 on a
     *               SUN station 3/60). The line search routine will terminate if the
     *               relative width of the interval of uncertainty is less than
     *               <code>xtol</code>.
     * @param iflag  This must be set to 0 on initial entry to <code>lbfgs_cimpl</code>.
     *               A return with <code>iflag &lt; 0</code> indicates an error,
     *               and <code>iflag = 0</code> indicates that the routine has
     *               terminated without detecting errors. On a return with
     *               <code>iflag = 1</code>, the user must evaluate the function
     *               <code>f</code> and gradient <code>g</code>. On a return with
     *               <code>iflag = 2</code>, the user must provide the diagonal matrix
     *               <code>Hk0</code>.
     *               <p>
     *               The following negative values of <code>iflag</code>, detecting an error,
     *               are possible:
     *               <ul>
     *               <li> <code>iflag = -1</code> The line search routine
     *               <code>mcsrch</code> failed. One of the following messages
     *               is printed:
     *               <ul>
     *               <li> Improper input parameters.
     *               <li> Relative width of the interval of uncertainty is at
     *               most <code>xtol</code>.
     *               <li> More than 20 function evaluations were required at the
     *               present iteration.
     *               <li> The step is too small.
     *               <li> The step is too large.
     *               <li> Rounding errors prevent further progress. There may not
     *               be  a step which satisfies the sufficient decrease and
     *               curvature conditions. Tolerances may be too small.
     *               </ul>
     *               <li><code>iflag = -2</code> The i-th diagonal element of the diagonal inverse
     *               Hessian approximation, given in DIAG, is not positive.
     *               <li><code>iflag = -3</code> Improper input parameters for LBFGS
     *               (<code>n</code> or <code>m</code> are not positive).
     *               </ul>
     * @throws ExceptionWithIflag
     */

    public void lbfgs(int n, int m, double[] x, double f, double[] g, boolean diagco, double[] diag, int[] iprint, double eps, double xtol, int[] iflag) throws ExceptionWithIflag {
        boolean execute_entire_while_loop = false;

        if (w == null || w.length != n * (2 * m + 1) + 2 * m) {
            w = new double[n * (2 * m + 1) + 2 * m];
        }

        if (iflag[0] == 0) {
            // Initialize.

            solution_cache = new double[n];
            System.arraycopy(x, 0, solution_cache, 0, n);

            iter = 0;

            if (n <= 0 || m <= 0) {
                iflag[0] = -3;
                throw new ExceptionWithIflag(iflag[0], "Improper input parameters  (n or m are not positive.)");
            }

            if (gtol <= 0.0001) {
                System.err.println("LBFGS.lbfgs_cimpl: gtol is less than or equal to 0.0001. It has been reset to 0.9.");
                gtol = 0.9;
            }

            nfun = 1;
            point = 0;
            finish = false;

            if (diagco) {
                for (i = 1; i <= n; i += 1) {
                    if (diag[i - 1] <= 0) {
                        iflag[0] = -2;
                        throw new ExceptionWithIflag(iflag[0], "The " + i + "-th diagonal element of the inverse hessian approximation is not positive.");
                    }
                }
            } else {
                for (i = 1; i <= n; i += 1) {
                    diag[i - 1] = 1;
                }
            }
            ispt = n + 2 * m;
            iypt = ispt + n * m;

            for (i = 1; i <= n; i += 1) {
                w[ispt + i - 1] = -g[i - 1] * diag[i - 1];
            }

            gnorm = Math.sqrt(ddot(n, g, 0, 1, g, 0, 1));
            stp1 = 1 / gnorm;
            ftol = 0.0001;
            maxfev = 20;

            if (iprint[0] >= 0) lb1(iprint, iter, nfun, gnorm, n, m, x, f, g, stp, finish);

            execute_entire_while_loop = true;
        }

        while (true) {
            if (execute_entire_while_loop) {
                iter = iter + 1;
                info[0] = 0;
                bound = iter - 1;
                if (iter != 1) {
                    if (iter > m) bound = m;
                    ys = ddot(n, w, iypt + npt, 1, w, ispt + npt, 1);
                    if (!diagco) {
                        yy = ddot(n, w, iypt + npt, 1, w, iypt + npt, 1);

                        for (i = 1; i <= n; i += 1) {
                            diag[i - 1] = ys / yy;
                        }
                    } else {
                        iflag[0] = 2;
                        return;
                    }
                }
            }

            if (execute_entire_while_loop || iflag[0] == 2) {
                if (iter != 1) {
                    if (diagco) {
                        for (i = 1; i <= n; i += 1) {
                            if (diag[i - 1] <= 0) {
                                iflag[0] = -2;
                                throw new ExceptionWithIflag(iflag[0], "The " + i + "-th diagonal element of the inverse hessian approximation is not positive.");
                            }
                        }
                    }
                    cp = point;
                    if (point == 0) cp = m;
                    w[n + cp - 1] = 1 / ys;

                    for (i = 1; i <= n; i += 1) {
                        w[i - 1] = -g[i - 1];
                    }

                    cp = point;

                    for (i = 1; i <= bound; i += 1) {
                        cp = cp - 1;
                        if (cp == -1) cp = m - 1;
                        sq = ddot(n, w, ispt + cp * n, 1, w, 0, 1);
                        inmc = n + m + cp + 1;
                        iycn = iypt + cp * n;
                        w[inmc - 1] = w[n + cp + 1 - 1] * sq;
                        daxpy(n, -w[inmc - 1], w, iycn, 1, w, 0, 1);
                    }

                    for (i = 1; i <= n; i += 1) {
                        w[i - 1] = diag[i - 1] * w[i - 1];
                    }

                    for (i = 1; i <= bound; i += 1) {
                        yr = ddot(n, w, iypt + cp * n, 1, w, 0, 1);
                        beta = w[n + cp + 1 - 1] * yr;
                        inmc = n + m + cp + 1;
                        beta = w[inmc - 1] - beta;
                        iscn = ispt + cp * n;
                        daxpy(n, beta, w, iscn, 1, w, 0, 1);
                        cp = cp + 1;
                        if (cp == m) cp = 0;
                    }

                    for (i = 1; i <= n; i += 1) {
                        w[ispt + point * n + i - 1] = w[i - 1];
                    }
                }

                nfev[0] = 0;
                stp[0] = 1;
                if (iter == 1) stp[0] = stp1;

                for (i = 1; i <= n; i += 1) {
                    w[i - 1] = g[i - 1];
                }
            }

            Mcsrch.getInstance().mcsrch(n, x, f, g, w, ispt + point * n, stp, ftol, xtol, maxfev, info, nfev, diag);

            if (info[0] == -1) {
                iflag[0] = 1;
                return;
            }

            if (info[0] != 1) {
                iflag[0] = -1;
                throw new ExceptionWithIflag(iflag[0], "Line search failed. See documentation of routine mcsrch. Error return of line search: info = " + info[0] + " Possible causes: function or gradient are incorrect, or incorrect tolerances.");
            }

            nfun = nfun + nfev[0];
            npt = point * n;

            for (i = 1; i <= n; i += 1) {
                w[ispt + npt + i - 1] = stp[0] * w[ispt + npt + i - 1];
                w[iypt + npt + i - 1] = g[i - 1] - w[i - 1];
            }

            point = point + 1;
            if (point == m) point = 0;

            gnorm = Math.sqrt(ddot(n, g, 0, 1, g, 0, 1));
            xnorm = Math.sqrt(ddot(n, x, 0, 1, x, 0, 1));
            xnorm = Math.max(1.0, xnorm);

            if (gnorm / xnorm <= eps) finish = true;

            if (iprint[0] >= 0) lb1(iprint, iter, nfun, gnorm, n, m, x, f, g, stp, finish);

            // Cache the current solution vector. Due to the spaghetti-like
            // nature of this code, it's not possible to quit here and return;
            // we need to go back to the top of the loop, and eventually call
            // mcsrch one more time -- but that will modify the solution vector.
            // So we need to keep a copy of the solution vector as it was at
            // the completion (info[0]==1) of the most recent line search.

            System.arraycopy(x, 0, solution_cache, 0, n);

            if (finish) {
                iflag[0] = 0;
                return;
            }

            execute_entire_while_loop = true;        // from now on, execute whole loop
        }
    }

    /**
     * Print debugging and status messages for <code>lbfgs_cimpl</code>.
     * Depending on the parameter <code>iprint</code>, this can include
     * number of function evaluations, current function value, etc.
     * The messages are output to <code>System.err</code>.
     *
     * @param iprint Specifies output generated by <code>lbfgs_cimpl</code>.<p>
     *               <code>iprint[0]</code> specifies the frequency of the output:
     *               <ul>
     *               <li> <code>iprint[0] &lt; 0</code>: no output is generated,
     *               <li> <code>iprint[0] = 0</code>: output only at first and last iteration,
     *               <li> <code>iprint[0] &gt; 0</code>: output every <code>iprint[0]</code> iterations.
     *               </ul><p>
     *               <p>
     *               <code>iprint[1]</code> specifies the type of output generated:
     *               <ul>
     *               <li> <code>iprint[1] = 0</code>: iteration count, number of function
     *               evaluations, function value, norm of the gradient, and steplength,
     *               <li> <code>iprint[1] = 1</code>: same as <code>iprint[1]=0</code>, plus vector of
     *               variables and  gradient vector at the initial point,
     *               <li> <code>iprint[1] = 2</code>: same as <code>iprint[1]=1</code>, plus vector of
     *               variables,
     *               <li> <code>iprint[1] = 3</code>: same as <code>iprint[1]=2</code>, plus gradient vector.
     *               </ul>
     * @param iter   Number of iterations so far.
     * @param nfun   Number of function evaluations so far.
     * @param gnorm  Norm of gradient at current solution <code>x</code>.
     * @param n      Number of free parameters.
     * @param m      Number of corrections kept.
     * @param x      Current solution.
     * @param f      Function value at current solution.
     * @param g      Gradient at current solution <code>x</code>.
     * @param stp    Current stepsize.
     * @param finish Whether this method should print the ``we're done'' message.
     */
    public void lb1(int[] iprint, int iter, int nfun, double gnorm, int n, int m, double[] x, double f, double[] g, double[] stp, boolean finish) {
        int i;

        if (iter == 0) {
            System.err.println("*************************************************");
            System.err.println("  n = " + n + "   number of corrections = " + m + "\n       initial values");
            System.err.println(" f =  " + f + "   gnorm =  " + gnorm);
            if (iprint[2 - 1] >= 1) {
                System.err.print(" vector x =");
                for (i = 1; i <= n; i++)
                    System.err.print("  " + x[i - 1]);
                System.err.println();

                System.err.print(" gradient vector g =");
                for (i = 1; i <= n; i++)
                    System.err.print("  " + g[i - 1]);
                System.err.println();
            }
            System.err.println("*************************************************");
            System.err.println("\ti\tnfn\tfunc\tgnorm\tsteplength");
        } else {
            if ((iprint[0] == 0) && (iter != 1 && !finish)) return;
            if (iprint[0] != 0) {
                if ((iter - 1) % iprint[0] == 0 || finish) {
                    if (iprint[2 - 1] > 1 && iter > 1)
                        System.err.println("\ti\tnfn\tfunc\tgnorm\tsteplength");
                    System.err.println("\t" + iter + "\t" + nfun + "\t" + f + "\t" + gnorm + "\t" + stp[0]);
                } else {
                    return;
                }
            } else {
                if (iprint[2 - 1] > 1 && finish)
                    System.err.println("\ti\tnfn\tfunc\tgnorm\tsteplength");
                System.err.println("\t" + iter + "\t" + nfun + "\t" + f + "\t" + gnorm + "\t" + stp[0]);
            }
            if (iprint[2 - 1] == 2 || iprint[2 - 1] == 3) {
                if (finish) {
                    System.err.print(" final point x =");
                } else {
                    System.err.print(" vector x =  ");
                }
                for (i = 1; i <= n; i++)
                    System.err.print("  " + x[i - 1]);
                System.err.println();
                if (iprint[2 - 1] == 3) {
                    System.err.print(" gradient vector g =");
                    for (i = 1; i <= n; i++)
                        System.err.print("  " + g[i - 1]);
                    System.err.println();
                }
            }
            if (finish)
                System.err.println(" The minimization terminated without detecting errors. iflag = 0");
        }
    }

    /**
     * Compute the sum of a vector times a scalara plus another vector.
     * Adapted from the subroutine <code>daxpy</code> in <code>lbfgs_cimpl.f</code>.
     * There could well be faster ways to carry out this operation; this
     * code is a straight translation from the Fortran.
     */
    public void daxpy(int n, double da, double[] dx, int ix0, int incx, double[] dy, int iy0, int incy) {
        int i, ix, iy, m, mp1;

        if (n <= 0) return;

        if (da == 0) return;

        if (!(incx == 1 && incy == 1)) {
            ix = 1;
            iy = 1;

            if (incx < 0) ix = (-n + 1) * incx + 1;
            if (incy < 0) iy = (-n + 1) * incy + 1;

            for (i = 1; i <= n; i += 1) {
                dy[iy0 + iy - 1] = dy[iy0 + iy - 1] + da * dx[ix0 + ix - 1];
                ix = ix + incx;
                iy = iy + incy;
            }

            return;
        }

        m = n % 4;
        if (m != 0) {
            for (i = 1; i <= m; i += 1) {
                dy[iy0 + i - 1] = dy[iy0 + i - 1] + da * dx[ix0 + i - 1];
            }

            if (n < 4) return;
        }

        mp1 = m + 1;
        for (i = mp1; i <= n; i += 4) {
            dy[iy0 + i - 1] = dy[iy0 + i - 1] + da * dx[ix0 + i - 1];
            dy[iy0 + i + 1 - 1] = dy[iy0 + i + 1 - 1] + da * dx[ix0 + i + 1 - 1];
            dy[iy0 + i + 2 - 1] = dy[iy0 + i + 2 - 1] + da * dx[ix0 + i + 2 - 1];
            dy[iy0 + i + 3 - 1] = dy[iy0 + i + 3 - 1] + da * dx[ix0 + i + 3 - 1];
        }
    }

    /**
     * Compute the dot product of two vectors.
     * Adapted from the subroutine <code>ddot</code> in <code>lbfgs_cimpl.f</code>.
     * There could well be faster ways to carry out this operation; this
     * code is a straight translation from the Fortran.
     */
    public double ddot(int n, double[] dx, int ix0, int incx, double[] dy, int iy0, int incy) {
        double dtemp;
        int i, ix, iy, m, mp1;

        dtemp = 0;

        if (n <= 0) return 0;

        if (!(incx == 1 && incy == 1)) {
            ix = 1;
            iy = 1;
            if (incx < 0) ix = (-n + 1) * incx + 1;
            if (incy < 0) iy = (-n + 1) * incy + 1;
            for (i = 1; i <= n; i += 1) {
                dtemp = dtemp + dx[ix0 + ix - 1] * dy[iy0 + iy - 1];
                ix = ix + incx;
                iy = iy + incy;
            }
            return dtemp;
        }

        m = n % 5;
        if (m != 0) {
            for (i = 1; i <= m; i += 1) {
                dtemp = dtemp + dx[ix0 + i - 1] * dy[iy0 + i - 1];
            }
            if (n < 5) return dtemp;
        }

        mp1 = m + 1;
        for (i = mp1; i <= n; i += 5) {
            dtemp = dtemp + dx[ix0 + i - 1] * dy[iy0 + i - 1] + dx[ix0 + i + 1 - 1] * dy[iy0 + i + 1 - 1] + dx[ix0 + i + 2 - 1] * dy[iy0 + i + 2 - 1] + dx[ix0 + i + 3 - 1] * dy[iy0 + i + 3 - 1] + dx[ix0 + i + 4 - 1] * dy[iy0 + i + 4 - 1];
        }

        return dtemp;
    }

    /**
     * Specialized exception class for LBFGS; contains the
     * <code>iflag</code> value returned by <code>lbfgs_cimpl</code>.
     */

    public static class ExceptionWithIflag extends Exception {
        @Serial
        private static final long serialVersionUID = -7826713489112275104L;
        public final int iflag;

        public ExceptionWithIflag(int i, String s) {
            super(s);
            iflag = i;
        }

        public String toString() {
            return getMessage() + " (iflag == " + iflag + ")";
        }
    }
}
