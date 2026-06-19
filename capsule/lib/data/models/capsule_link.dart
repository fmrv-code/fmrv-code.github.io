import 'package:isar/isar.dart';

part 'capsule_link.g.dart';

@collection
class CapsuleLink {
  Id id = Isar.autoIncrement;

  @Index(unique: true, replace: true)
  late String url;

  // --- Metadata fetched from the page ---
  String? title;
  String? description;
  String? faviconUrl;
  String? imageUrl;
  String? siteName;

  /// Whether we've already attempted a metadata fetch for this URL
  bool metadataFetched = false;

  // --- Organisation ---
  @Index()
  int? capsuleId;

  List<int> tagIds = [];

  bool isFavorite = false;

  @Index()
  DateTime createdAt = DateTime.now();

  DateTime updatedAt = DateTime.now();

  // --- Display helpers (not persisted) ---
  @ignore
  String get displayTitle => title?.isNotEmpty == true ? title! : url;

  @ignore
  String get domain {
    try {
      return Uri.parse(url).host.replaceFirst('www.', '');
    } catch (_) {
      return url;
    }
  }
}
