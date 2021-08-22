package com.onthegomap.flatmap.config;

import com.onthegomap.flatmap.geo.GeoUtils;
import com.onthegomap.flatmap.geo.TileExtents;
import java.time.Duration;
import org.locationtech.jts.geom.Envelope;

public record CommonParams(
  // provided
  Envelope latLonBounds,
  int threads,
  Duration logInterval,
  int minzoom,
  int maxzoom,
  boolean deferIndexCreation,
  boolean optimizeDb,
  boolean emitTilesInOrder,
  boolean forceOverwrite,
  boolean gzipTempStorage,
  String nodeMapType,
  String nodeMapStorage,

  // computed
  Envelope worldBounds,
  TileExtents extents
) {

  public CommonParams(
    Envelope latLonBounds,
    int threads,
    Duration logInterval,
    int minzoom,
    int maxzoom,
    boolean deferIndexCreation,
    boolean optimizeDb,
    boolean emitTilesInOrder,
    boolean forceOverwrite,
    boolean gzipTempStorage,
    String nodeMapType,
    String nodeMapStorage
  ) {
    this(
      latLonBounds,
      threads,
      logInterval,
      minzoom,
      maxzoom,
      deferIndexCreation,
      optimizeDb,
      emitTilesInOrder,
      forceOverwrite,
      gzipTempStorage,
      nodeMapType,
      nodeMapStorage,

      // computed
      GeoUtils.toWorldBounds(latLonBounds),
      TileExtents.computeFromWorldBounds(maxzoom, GeoUtils.toWorldBounds(latLonBounds))
    );
  }

  public static final int MIN_MINZOOM = 0;
  public static final int MAX_MAXZOOM = 14;

  public CommonParams {
    if (minzoom > maxzoom) {
      throw new IllegalArgumentException("Minzoom cannot be greater than maxzoom");
    }
    if (minzoom < 0) {
      throw new IllegalArgumentException("Minzoom must be >= 0, was " + minzoom);
    }
    if (maxzoom > 14) {
      throw new IllegalArgumentException("Max zoom must be <= 14, was " + maxzoom);
    }
  }

  public static CommonParams defaults() {
    return from(Arguments.of());
  }

  public static CommonParams from(Arguments arguments) {
    return from(arguments, BoundsProvider.WORLD);
  }

  public static CommonParams from(Arguments arguments, BoundsProvider defaultBounds) {
    return new CommonParams(
      arguments.bounds("bounds", "bounds", defaultBounds),
      arguments.threads(),
      arguments.duration("loginterval", "time between logs", "10s"),
      arguments.integer("minzoom", "minimum zoom level", MIN_MINZOOM),
      arguments.integer("maxzoom", "maximum zoom level (limit 14)", MAX_MAXZOOM),
      arguments.get("defer_mbtiles_index_creation", "add index to mbtiles file after finished writing", false),
      arguments.get("optimize_db", "optimize mbtiles after writing", false),
      arguments.get("emit_tiles_in_order", "emit tiles in index order", true),
      arguments.get("force", "force overwriting output file", false),
      arguments.get("gzip_temp", "gzip temporary feature storage (uses more CPU, but less disk space)", false),
      arguments.get("nodemap_type", "type of node location map: noop, sortedtable, or sparsearray", "sortedtable"),
      arguments.get("nodemap_storage", "storage for location map: mmap or ram", "mmap")
    );
  }
}