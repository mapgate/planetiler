package com.onthegomap.flatmap;

import com.onthegomap.flatmap.geo.GeometryException;
import com.onthegomap.flatmap.mbiles.Mbtiles;
import com.onthegomap.flatmap.reader.SourceFeature;
import com.onthegomap.flatmap.reader.osm.OsmElement;
import com.onthegomap.flatmap.reader.osm.OsmReader;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defines the logic for how maps are generated including:
 * <ul>
 *   <li>How source features (OSM elements, shapefile elements, etc.) map to output features and their tags</li>
 *   <li>How output vector tile features on an output tile should be post-processed (see {@link FeatureMerge})</li>
 *   <li>What attributes to include in the mbtiles metadata output (see {@link Mbtiles})</li>
 *   <li>Whether {@link Wikidata} class should fetch wikidata translations for an OSM element</li>
 * </ul>
 * <p>
 * {@link Profile#processFeature(SourceFeature, FeatureCollector)} only handles a single element at a time. To "join"
 * elements across or within sources, implementations can store data in instance fields and wait to process them until
 * an element is encountered in a later source, or the {@link Profile#finish(String, FeatureCollector.Factory, Consumer)}
 * method is called for a source. All methods may be called concurrently by multiple threads, so implementations must be
 * careful to ensure access to instance fields is thread-safe.
 */
public interface Profile {

  /**
   * Extracts information from <a * href="https://wiki.openstreetmap.org/wiki/Relation">OSM relations</a> that will be
   * passed along to {@link #processFeature(SourceFeature, FeatureCollector)} for any OSM element in that relation.
   * <p>
   * The result of this method is stored in memory.
   *
   * @param relation the OSM relation
   * @return a list of relation info instances with information extracted from the relation to pass to {@link
   * #processFeature(SourceFeature, FeatureCollector)}, or {@code null} to ignore.
   * @implNote The default implementation returns {@code null} to ignore all relations
   */
  default List<OsmReader.RelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    return null;
  }

  /**
   * Generates output features for any input feature that should appear in the map.
   * <p>
   * Multiple threads may invoke this method concurrently for a single data source so implementations should ensure
   * thread-safe access to any shared data structures.  Separate data sources are processed sequentially.
   * <p>
   * All OSM nodes are processed first, then ways, then relations.
   *
   * @param sourceFeature the input feature from a source dataset (OSM element, shapefile element, etc.)
   * @param features      a collector for generating output map features to emit
   */
  void processFeature(SourceFeature sourceFeature, FeatureCollector features);

  /**
   * Free any resources associated with this profile (ie. shared data structures)
   */
  default void release() {
  }

  /**
   * Apply any post-processing to features in an output layer of a tile before writing it to the output file
   * <p>
   * These transformations may add, remove, or change the tags, geometry, or ordering of output features based on other
   * features present in this tile. See {@link FeatureMerge} class for a set of common transformations that merge
   * linestrings/polygons.
   * <p>
   * Many threads invoke this method concurrently so ensure thread-safe access to any shared data structures.
   *
   * @param layer the output layer name
   * @param zoom  zoom level of the tile
   * @param items all of the output features in this layer in this tile
   * @return the new list of output features or {@code null} to not change anything
   * @throws GeometryException for any recoverable geometric operation failures - the framework will log the error, emit
   *                           the original input features, and continue processing other tiles
   * @implSpec The default implementation passes through input features unaltered
   */
  default List<VectorTileEncoder.Feature> postProcessLayerFeatures(String layer, int zoom,
    List<VectorTileEncoder.Feature> items) throws GeometryException {
    return items;
  }

  /**
   * @return the name of the generated tileset to put into {@link Mbtiles} metadata
   * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata>MBTiles specification</a>
   */
  String name();

  /**
   * @return the description of the generated tileset to put into {@link Mbtiles} metadata
   * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata>MBTiles specification</a>
   */
  default String description() {
    return null;
  }

  /**
   * @return the attribution of the generated tileset to put into {@link Mbtiles} metadata
   * @see <a href="https://www.openstreetmap.org/copyright">https://www.openstreetmap.org/copyright</a> for attribution
   * requirements of any map using OpenStreetMap data
   * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata>MBTiles specification</a>
   */
  default String attribution() {
    return null;
  }

  /**
   * @return the version of the generated tileset to put into {@link Mbtiles} metadata
   * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata>MBTiles specification</a>
   */
  default String version() {
    return null;
  }

  /**
   * @return {@code true} to set {@code type="overlay"} in {@link Mbtiles} metadata otherwise sets {@code
   * type="baselayer"}
   * @implSpec The default implementation sets {@code type="baselayer"}
   * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata>MBTiles specification</a>
   */
  default boolean isOverlay() {
    return false;
  }

  /**
   * Defines whether {@link Wikidata} should fetch wikidata translations for the input element.
   *
   * @param elem the input OSM element
   * @return {@code true} to fetch wikidata translations for {@code elem}, {@code false} to ignore
   */
  default boolean caresAboutWikidataTranslation(OsmElement elem) {
    return true;
  }

  /**
   * Invoked once for each source after all elements for that source have been processed.
   *
   * @param sourceName        the name of the source that just finished
   * @param featureCollectors a supplier for new {@link FeatureCollector} instances for a {@link SourceFeature}.
   * @param next              a consumer to pass finished map features to
   */
  default void finish(String sourceName, FeatureCollector.Factory featureCollectors,
    Consumer<FeatureCollector.Feature> next) {
  }

  /**
   * @param name the input source name
   * @return {@code true} if this profile uses that source, {@code false} if it is safe to ignore
   */
  default boolean caresAboutSource(String name) {
    return true;
  }

  /**
   * A default implementation of {@link Profile} that emits no output elements.
   */
  class NullProfile implements Profile {

    @Override
    public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    }

    @Override
    public List<VectorTileEncoder.Feature> postProcessLayerFeatures(String layer, int zoom,
      List<VectorTileEncoder.Feature> items) {
      return items;
    }

    @Override
    public String name() {
      return "Null";
    }
  }
}