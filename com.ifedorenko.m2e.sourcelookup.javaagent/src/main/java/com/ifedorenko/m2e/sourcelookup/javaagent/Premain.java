/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.javaagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class Premain
{
    public static void premain( String agentArgs, Instrumentation inst )
    {
        System.err.println( "Dynamic source lookup support loaded." );

        inst.addTransformer( new ClassFileTransformer()
        {
            public byte[] transform( ClassLoader loader, final String className, Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain, byte[] classfileBuffer )
                throws IllegalClassFormatException
            {
                try
                {
                    if ( protectionDomain == null )
                    {
                        return null;
                    }

                    final CodeSource codeSource = protectionDomain.getCodeSource();
                    if ( codeSource == null )
                    {
                        return null;
                    }

                    final URL locationUrl = codeSource.getLocation();
                    if ( locationUrl == null )
                    {
                        return null;
                    }

                    final String location = locationUrl.toExternalForm();

                    final ClassReader r = new ClassReader( classfileBuffer, 0, classfileBuffer.length );
                    final ClassWriter w = new ClassWriter( 0 );

                    r.accept( new ClassVisitor( Opcodes.ASM4, w )
                    {
                        public void visitSource( String source, String debug )
                        {
                            if ( debug != null )
                            {
                                System.err.println( "m2e SMAP merge is not supported!" );
                                System.err.println( debug );
                            }
                            else
                            {
                                StringBuilder smap = new StringBuilder();
                                smap.append( "SMAP\n" );
                                smap.append( source ).append( "\n" );
                                smap.append( "Java\n" );
                                smap.append( "*S m2e\n" );
                                smap.append( "*F\n" );
                                smap.append( "1 " ).append( location ).append( "\n" );
                                smap.append( "*L\n" );// JDT insists on LineSection
                                smap.append( "*E\n" );
                                debug = smap.toString();
                            }

                            super.visitSource( source, debug );
                        };

                    }, 0 );

                    return w.toByteArray();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
                return null;
            }
        } );
    }
}
