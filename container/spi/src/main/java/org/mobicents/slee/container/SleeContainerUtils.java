/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * Created on May 23, 2005
 * 
 * The Open SLEE Project
 * 
 * A SLEE for the People
 * 
 * The source code contained in this file is in in the public domain.          
 * It can be used in any project or product without prior permission, 	      
 * license or royalty payments. There is no claim of correctness and
 * NO WARRANTY OF ANY KIND provided with this code.
 */
package org.mobicents.slee.container;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 
 * TODO Class Description
 * 
 * @author F.Moggia
 */

public class SleeContainerUtils {

	public static String toHex(String str) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			if (Character.isLetter(str.charAt(i))/*|| str.charAt(i) == '-'*/
					|| Character.isDigit(str.charAt(i))) {
				buff.append(str.charAt(i));
			} else {
				buff.append("\\u00");
				buff.append(Integer.toHexString(str.charAt(i)));
			}
		}

		return buff.toString();
	}

	public static String fromHex(String str) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\\') {
				buff.append(Integer.valueOf(str.substring(i + 4, i + 6), 16));
			} else {
				buff.append(str.charAt(i));
			}
		}
		//logger.debug(buff);
		return buff.toString();
	}

	/**
	 * Retrieves the current thread {@link ClassLoader}, securely. 
	 * @return
	 */
	public static ClassLoader getCurrentThreadClassLoader() {
		if (System.getSecurityManager()!=null)
			return AccessController
					.doPrivileged(new PrivilegedAction<ClassLoader>() {
						public ClassLoader run() {
							return Thread.currentThread()
									.getContextClassLoader();
						}
					});
		else
			return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Sets the current thread class loader securely
	 * @param classLoader
	 */
	public static void setCurrentThreadClassLoader(final ClassLoader classLoader) {
		if (System.getSecurityManager() != null)
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        else
            Thread.currentThread().setContextClassLoader(classLoader);
	}
}
