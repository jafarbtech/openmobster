/******************************************************************************
 * OpenMobster                                                                *
 * Copyright 2008, OpenMobster, and individual                                *
 * contributors as indicated by the @authors tag. See the                     *
 * copyright.txt in the distribution for a full listing of                    *
 * individual contributors.                                                   *
 *                                                                            *
 * This is free software; you can redistribute it and/or modify it            *
 * under the terms of the GNU Lesser General Public License as                *
 * published by the Free Software Foundation; either version 2.1 of           *
 * the License, or (at your option) any later version.                        *
 *                                                                            *
 * This software is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU           *
 * Lesser General Public License for more details.                            *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public           *
 * License along with this software; if not, write to the Free                *
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA         *
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.                   *
 ******************************************************************************/
package org.openmobster.core.mobileCloud.test.cloud;

import test.openmobster.core.mobileCloud.rimos.testsuite.Test;

import org.openmobster.core.mobileCloud.rimos.module.bus.Bus;
import org.openmobster.core.mobileCloud.rimos.module.bus.Invocation;
import org.openmobster.core.mobileCloud.rimos.module.bus.InvocationResponse;
import org.openmobster.core.mobileCloud.rimos.module.mobileObject.*;

/**
 * @author openmobster@gmail.com
 *
 */
public final class TestMobileObjectSharing extends Test 
{
	public void runTest()
	{
		try
		{
			MobileObject mobileObject = new MobileObject();
			mobileObject.setRecordId("unique-1");
			mobileObject.setStorageId("test.sharing");
			mobileObject.setValue("blah", "blah");
			
			String recordId = MobileObjectDatabase.getInstance().create(mobileObject);
			
			//Assertion
			System.out.println("New Record Created with Id="+recordId);
			assertNotNull(recordId, this.getClass().getName()+":/RecordIdIsNull");
			assertEquals(recordId,"unique-1", this.getClass().getName()+":/RecordIdMismatch");
			
			//Read the object locally
			mobileObject = MobileObjectDatabase.getInstance().read("test.sharing", "unique-1");
			String localResult = mobileObject.getValue("blah"); 
			System.out.println("LocalResult------------------------------------");
			System.out.println(localResult);
			System.out.println("------------------------------------------------");
			assertEquals(localResult, "blah", this.getClass()+"://Local/ObjectValueMismatch");
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
}
