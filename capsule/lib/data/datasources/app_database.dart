import 'dart:convert';
import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

part 'app_database.g.dart';

// ─── TypeConverter ────────────────────────────────────────────────────────────

class IntListConverter extends TypeConverter<List<int>, String> {
  const IntListConverter();

  @override
  List<int> fromSql(String fromDb) =>
      (jsonDecode(fromDb) as List).cast<int>();

  @override
  String toSql(List<int> value) => jsonEncode(value);
}

// ─── Tables ───────────────────────────────────────────────────────────────────

class Tags extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text().unique()();
  IntColumn get colorValue =>
      integer().withDefault(const Constant(0xFF007AFF))();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
}

class Capsules extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  IntColumn get iconCodePoint =>
      integer().withDefault(const Constant(0xe2c7))();
  IntColumn get colorValue =>
      integer().withDefault(const Constant(0xFF007AFF))();
  IntColumn get noteCount => integer().withDefault(const Constant(0))();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt =>
      dateTime().withDefault(currentDateAndTime)();
}

class Notes extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get title => text()();
  TextColumn get content => text().withDefault(const Constant(''))();
  IntColumn get capsuleId => integer().nullable()();
  TextColumn get tagIds => text()
      .map(const IntListConverter())
      .withDefault(const Constant('[]'))();
  BoolColumn get isPinned =>
      boolean().withDefault(const Constant(false))();
  BoolColumn get isFavorite =>
      boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt =>
      dateTime().withDefault(currentDateAndTime)();
}

class CapsuleLinks extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get url => text().unique()();
  TextColumn get title => text().nullable()();
  TextColumn get description => text().nullable()();
  TextColumn get faviconUrl => text().nullable()();
  TextColumn get imageUrl => text().nullable()();
  TextColumn get siteName => text().nullable()();
  BoolColumn get metadataFetched =>
      boolean().withDefault(const Constant(false))();
  IntColumn get capsuleId => integer().nullable()();
  TextColumn get tagIds => text()
      .map(const IntListConverter())
      .withDefault(const Constant('[]'))();
  BoolColumn get isFavorite =>
      boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt =>
      dateTime().withDefault(currentDateAndTime)();
}

// ─── Database ─────────────────────────────────────────────────────────────────

@DriftDatabase(tables: [Tags, Capsules, Notes, CapsuleLinks])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dir = await getApplicationDocumentsDirectory();
    final file = File(p.join(dir.path, 'capsule.sqlite'));
    return NativeDatabase.createInBackground(file);
  });
}
