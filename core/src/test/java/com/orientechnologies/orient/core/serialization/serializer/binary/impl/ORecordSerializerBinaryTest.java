/*
 * Copyright 2018 OrientDB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.serialization.serializer.binary.impl;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.serialization.types.OIntegerSerializer;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.BytesContainer;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerBinary;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.OResultBinary;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.OVarIntSerializer;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mdjurovi
 */
public class ORecordSerializerBinaryTest {
  
  static ODatabaseDocumentTx db;
  static ORecordSerializerBinary serializer;
  
  @BeforeClass
  public static void initTestDatabase(){
    db = new ODatabaseDocumentTx("memory:test").create();
    db.createClass("TestClass");
    db.command(new OCommandSQL("create property TestClass.TestEmbedded EMBEDDED")).execute();
    serializer = new ORecordSerializerBinary();
  }
  
  @AfterClass
  public static void dropDatabase(){
    if (db != null){
      db.drop();
    }
  }
  
  @Test
  public void testGetTypedFiledSimple(){    
    ODocument doc = new ODocument();
    Integer setValue = 16;
    doc.setProperty("TestField", setValue);
    byte[] serializedDoc = serializer.toStream(doc, false);
    Integer value = serializer.deserializeField(serializedDoc, null, "TestField");
    Assert.assertEquals(setValue, value);
  }
  
  protected static String stringFromBytes(final byte[] bytes, final int offset, final int len) {
    try {
      return new String(bytes, offset, len, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw OException.wrapException(new OSerializationException("Error on string decoding"), e);
    }
  }
  
  protected static String readString(final BytesContainer bytes) {
    final int len = OVarIntSerializer.readAsInteger(bytes);
    final String res = stringFromBytes(bytes.bytes, bytes.offset, len);
    bytes.skip(len);
    return res;
  }
  
  protected static int readInteger(final BytesContainer container) {
    final int value = OIntegerSerializer.INSTANCE.deserializeLiteral(container.bytes, container.offset);
    container.offset += OIntegerSerializer.INT_SIZE;
    return value;
  }
  
  @Test
  public void testGetTypedFieldEmbedded(){
    ODocument root = new ODocument();
    ODocument embedded = new ODocument("TestClass");
    Integer setValue = 17;
    embedded.setProperty("TestField", setValue);    
//    embedded = db.save(embedded);
    
    OClass classOfEmbedded = db.getClass("TestClass");
    
    root.field("TestEmbedded", embedded);
    root.setClassName("TestClass");
        
    db.save(root);    
    
    byte[] rootBytes = serializer.toStream(root, false);
    byte[] embeddedNativeBytes = serializer.toStream(embedded, false);
    //want to update data pointers because first byte will be removed
    decreasePositionsBy(embeddedNativeBytes, 1);
    //skip serializer version
    embeddedNativeBytes = Arrays.copyOfRange(embeddedNativeBytes, 1, embeddedNativeBytes.length);        
    
    
    OResultBinary embeddedBytesViaGet = serializer.deserializeField(rootBytes, classOfEmbedded, "TestEmbedded");    
    Assert.assertTrue(Arrays.equals(embeddedNativeBytes, embeddedBytesViaGet.getResultBytes()));
  }
  
  public void testGetTypedFilledFromEmbedded(){
    
  }

  private void decreasePositionsBy(byte[] embeddedNativeBytes, int stepSize) {
    BytesContainer container = new BytesContainer(embeddedNativeBytes);
    container.offset++;
    if (serializer.getCurrentSerializer().isSerializingClassNameByDefault()){
      readString(container);
    }
    int len = 1;
    while (len != 0){
      len = OVarIntSerializer.readAsInteger(container);
      if (len > 0){
        //read field name
        container.offset += len;
        //read data pointer
        int pointer = readInteger(container);
        //shift pointer by start ofset
        pointer -= stepSize;
        //write to byte container
        OIntegerSerializer.INSTANCE.serializeLiteral(pointer, container.bytes, container.offset - OIntegerSerializer.INT_SIZE);
        //read type
        container.offset++;
      }
      else if (len < 0){
        //rtead data pointer
        int pointer = readInteger(container);
        //shift pointer
        pointer -= stepSize;
        //write to byte container
        OIntegerSerializer.INSTANCE.serializeLiteral(pointer, container.bytes, container.offset - OIntegerSerializer.INT_SIZE);
      }
    }
  }
  
}
