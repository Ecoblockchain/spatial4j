/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spatial4j.core.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import org.jeo.geom.GeomBuilder;
import org.jeo.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContextFactory;
import com.spatial4j.core.io.PolylineWriter.Encoder;
import com.spatial4j.core.io.jts.JtsPolylineWriter;
import com.spatial4j.core.shape.JtsGeometryTest;
import com.spatial4j.core.shape.Shape;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PolylineReadWriteTest {

  ShapeReader reader;
  ShapeWriter writer;
  
  GeometryFactory gf;
  GeomBuilder gb;
  JtsSpatialContext ctx;

  @Before
  public void setUp() {
    ctx = JtsSpatialContext.GEO;
    
    gb = new GeomBuilder();
    reader = ctx.getFormats().getReader(ShapeIO.POLY);
    writer = ctx.getFormats().getWriter(ShapeIO.POLY);

    Assert.assertNotNull(reader);
    Assert.assertNotNull(writer);
  }
  

  @Test
  public void testWriteTestJSON() throws Exception {
    
    //Fiji is a group of islands crossing the dateline.
    String wktFiji = JtsGeometryTest.readFirstLineFromRsrc("/fiji.wkt.txt");
    String wktRussia = JtsGeometryTest.readFirstLineFromRsrc("/russia.wkt.txt");

    
    ShapeWriter geojson = ctx.getFormats().getWriter(ShapeIO.GeoJSON);
    ShapeWriter wktwriter = ctx.getFormats().getWriter(ShapeIO.WKT);
    
    ArrayList<Shape> shapes = new ArrayList<Shape>();
    shapes.add(point());
    shapes.add(line());
    shapes.add(polygon1());
    shapes.add(polygon2());
    shapes.add(multiLine());
    shapes.add(multiPoint());
    shapes.add(polygon1().getBoundingBox());
  //  shapes.add(ctx.readShapeFromWkt(wktFiji)); // fiji
  //  shapes.add(ctx.readShapeFromWkt(wktRussia)); // fiji
    Iterator<Shape> iter = shapes.iterator();
    
    BinaryCodec codec = ctx.getBinaryCodec();
    NumberFormat nf = NumberFormat.getPercentInstance(Locale.ROOT);
    StringWriter out = new StringWriter();
    out.write("{ \"test\": [\n");
    while(iter.hasNext()) {
      Shape shape = iter.next();
      String enc = writer.toString(shape);
      String gjs = geojson.toString(shape);
      String wkt = wktwriter.toString(shape);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream bin = new DataOutputStream(bos);
      codec.writeShape(bin, shape);
      
      float perJ = enc.length()/(float)gjs.length();
      float perW = enc.length()/(float)wkt.length();
      float perB = enc.length()/(float)bos.size();
      
      out.write("{\"wkt\" :\""+wkt+"\",\n");
      out.write(" \"json\":"+gjs+",\n");
      out.write(" \"poly\":\""+enc+"\",\n");
      out.write(" \"compare_json\": \""+enc.length()+" vs "+gjs.length()+" chars. "+nf.format(perJ)+"\",\n");
      out.write(" \"compare_wkt\" : \""+enc.length()+" vs "+wkt.length()+" chars. "+nf.format(perW)+"\",\n");
      out.write(" \"compare_bin\" : \""+enc.length()+" vs "+bos.size()+" bytes. "+nf.format(perB)+"\"\n");
      out.write("}");
      if(iter.hasNext()) {
        out.write(",\n");
      }
    }
    out.write("]}");   
    
    System.out.println( out );
  }
  
  
  
  public void checkEqual(Shape expected, Shape actual) {
    // GeoJSON has limited numberic precision so the off by .0000001 does not affect its equals
    ShapeWriter writer = ctx.getFormats().getWriter(ShapeIO.GeoJSON);
    Assert.assertEquals(writer.toString(expected), writer.toString(actual));
  }
  
  @Test
  public void testWriteThenReadPoint() throws Exception {
    checkEqual(point(), reader.read(writer.toString(point())));
  }

  @Test
  public void testWriteThenReadLineString() throws Exception {
    checkEqual(line(), reader.read(writer.toString(line())));
  }

  @Test
  public void testWriteThenReadPolygon() throws Exception {
    checkEqual(polygon1(), reader.read(writer.toString(polygon1())));
    checkEqual(polygon2(), reader.read(writer.toString(polygon2())));
  }

  @Test
  public void testWriteThenReadMultiPoint() throws Exception {
    checkEqual(multiPoint(), reader.read(writer.toString(multiPoint())));
  }

  @Test
  public void testWriteThenReadMultiLineString() throws Exception {
    checkEqual(multiLine(), reader.read(writer.toString(multiLine())));
  }

  @Test
  public void testWriteThenReadMultiPolygon() throws Exception {
    checkEqual(multiPolygon(), reader.read(writer.toString(multiPolygon())));
  }

  @Test
  public void testWriteThenReadRectangle() throws Exception {
    checkEqual(polygon1().getBoundingBox(), reader.read(writer.toString(polygon1().getBoundingBox())));
  }
  
  
//  @Test
//  public void testParseGeometryCollection() throws Exception {
//    assertEquals(collection(), reader.read(collectionText(),true));
//  }
//
//  @Test
//  public void testEncodeGeometryCollection() throws Exception {
//    assertEquals(collectionText(), writer.toString(collection()));
//  }

  String pointText() {
    return strip("{'type': 'Point','coordinates':[100.1,0.1]}");
  }

  com.spatial4j.core.shape.Point point() {
    return ctx.makePoint(100.1, 0.1);
  }

  String lineText() {
    return strip(
      "{'type': 'LineString', 'coordinates': [[100.1,0.1],[101.1,1.1]]}");
  }

  Shape line() {
    return ctx.makeShape(gb.points(100.1, 0.1, 101.1,1.1).toLineString());
  }

  Shape polygon1() {
    return ctx.makeShape(gb.points(100.1, 0.1, 101.1, 0.1, 101.1, 1.1, 100.1, 1.1, 100.1, 0.1).ring().toPolygon());
  }

  String polygonText1() {
    return strip("{ 'type': 'Polygon',"+
    "'coordinates': ["+
    "  [ [100.1, 0.1], [101.1, 0.1], [101.1, 1.1], [100.1, 1.1], [100.1, 0.1] ]"+
    "  ]"+
     "}");
  }

  String polygonText2() {
    return strip("{ 'type': 'Polygon',"+
    "  'coordinates': ["+
    "    [ [100.1, 0.1], [101.1, 0.1], [101.1, 1.1], [100.1, 1.1], [100.1, 0.1] ],"+
    "    [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]"+
    "    ]"+
    "   }");
  }

  Shape polygon2() {
    return ctx.makeShape(gb.points(100.1, 0.1, 101.1, 0.1, 101.1, 1.1, 100.1, 1.1, 100.1, 0.1).ring()
      .points(100.2, 0.2, 100.8, 0.2, 100.8, 0.8, 100.2, 0.8, 100.2, 0.2).ring().toPolygon());
  }

  String multiPointText() {
    return strip(
      "{ 'type': 'MultiPoint',"+
        "'coordinates': [ [100.1, 0.1], [101.1, 1.1] ]"+
      "}");
  }

  Shape multiPoint() {
    return ctx.makeShape( gb.points(100.1, 0.1, 101.1, 1.1).toMultiPoint() );
  }
  

  String multiLineText() {
    return strip(
      "{ 'type': 'MultiLineString',"+
      "  'coordinates': ["+
      "    [ [100.1, 0.1], [101.1, 1.1] ],"+
      "    [ [102.1, 2.1], [103.1, 3.1] ]"+
      "    ]"+
      "  }");
  }

  Shape multiLine() {
    return ctx.makeShape(gb.points(100.1, 0.1, 101.1, 1.1).lineString()
      .points(102.1, 2.1, 103.1, 3.1).lineString().toMultiLineString());
  }
  
  String multiPolygonText() {
    return strip(
    "{ 'type': 'MultiPolygon',"+
    "  'coordinates': ["+
    "    [[[102.1, 2.1], [103.1, 2.1], [103.1, 3.1], [102.1, 3.1], [102.1, 2.1]]],"+
    "    [[[100.1, 0.1], [101.1, 0.1], [101.1, 1.1], [100.1, 1.1], [100.1, 0.1]],"+
    "     [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]"+
    "    ]"+
    "  }");
  }

  Shape multiPolygon() {
    return ctx.makeShape(
    gb.points(102.1, 2.1, 103.1, 2.1, 103.1, 3.1, 102.1, 3.1, 102.1, 2.1).ring().polygon()
      .points(100.1, 0.1, 101.1, 0.1, 101.1, 1.1, 100.1, 1.1, 100.1, 0.1).ring()
      .points(100.2, 0.2, 100.8, 0.2, 100.8, 0.8, 100.2, 0.8, 100.2, 0.2).ring().polygon()
      .toMultiPolygon());
  }

  String rectangleText() {
    return strip(
      "{" +
      "'type':'Polygon'," +
      "'coordinates': [[[100.1,0.1], [100.1,1.1], [101.1,1.1], [101.1,0.1], [100.1,0.1]]]" +
      "}");
  }

  String collectionText() {
    return strip(
      "{ 'type': 'GeometryCollection',"+
      "  'geometries': ["+
      "    { 'type': 'Point',"+
      "    'coordinates': [100.1, 0.1]"+
      "    },"+
      "    { 'type': 'LineString',"+
      "    'coordinates': [ [101.1, 0.1], [102.1, 1.1] ]"+
      "    }"+
      "  ]"+
      "  }");
  }

  Shape collection() {
    return ctx.makeShape(gb.point(100.1,0.1).point().points(101.1, 0.1, 102.1, 1.1).lineString().toCollection());
  }

  String strip(String json) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      if (c == ' ' || c == '\n') continue;
      if (c == '\'') {
        sb.append("\"");
      }
      else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
