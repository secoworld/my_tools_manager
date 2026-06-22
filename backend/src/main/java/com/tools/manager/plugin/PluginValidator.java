package com.tools.manager.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PluginValidator {

    private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    private static final Pattern SEMVER_PATTERN =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> VALID_ICON_NAMES = new HashSet<>(Arrays.asList(
            "Plus", "Minus", "Check", "Close", "CircleCheck", "CircleClose",
            "Warning", "WarningFilled", "QuestionFilled", "InfoFilled",
            "Remove", "CirclePlus", "CirclePlusFilled", "Search",
            "Edit", "EditPen", "Delete", "Document", "Folder", "FolderOpened",
            "Files", "Picture", "Pictures", "VideoCamera", "Microphone",
            "Headset", "TakeawayBox", "Monitor", "Mobile", "Iphone",
            "Bell", "BellFilled", "MuteNotification", "User", "UserFilled",
            "Star", "StarFilled", "Sunny", "Moon", "MoonNight",
            "Location", "LocationFilled", "Position", "Compass", "Guide",
            "MapLocation", "Place", "Aim", "CopyDocument", "DeleteDocument",
            "FilesFilled", "FolderAdd", "FolderChecked", "DocumentAdd",
            "DocumentChecked", "DocumentCopy", "DocumentDelete", "Download",
            "Upload", "UploadFilled", "View", "ZoomIn", "ZoomOut",
            "Refresh", "RefreshLeft", "RefreshRight", "Loading", "LoadingFilled",
            "Lock", "LockFilled", "Unlock", "Key", "UnlockFilled",
            "Setting", "Tools", "More", "MoreFilled", "Operation",
            "OperationFilled", "Menu", "Grid", "MenuFilled", "Histogram",
            "DataLine", "DataAnalysis", "PieChart", "TrendCharts", "Graph",
            "Filter", "FilterFilled", "Sort", "SortDown", "SortUp",
            "Top", "Bottom", "Right", "Back", "Left",
            "TopRight", "TopLeft", "BottomRight", "BottomLeft", "ArrowUp",
            "ArrowDown", "ArrowLeft", "ArrowRight", "CaretTop", "CaretBottom",
            "CaretLeft", "CaretRight", "DCaret", "SortFilled", "ArrowUpBold",
            "ArrowDownBold", "ArrowLeftBold", "ArrowRightBold", "ArrowUpFilled",
            "ArrowDownFilled", "ArrowLeftFilled", "ArrowRightFilled", "HomeFilled",
            "MenuUnfold", "MenuFold", "ToolsFilled", "SettingFilled",
            "StarOff", "Avatar", "UserFilled2", "Management", "Service",
            "ServiceFilled", "Promotion", "PromotionFilled", "ChatDotRound",
            "ChatDotSquare", "ChatLineRound", "ChatLineSquare", "ChatRound",
            "ChatSquare", "Comment", "CommentFilled", "Message", "MessageFilled",
            "Postcard", "PostcardFilled", "PositionFilled2", "Trophy",
            "TrophyFilled", "TrophyBase", "Medal", "MedalFilled", "GoldMedal",
            "GoldMedalFilled", "SilverMedal", "SilverMedalFilled", "BronzeMedal",
            "BronzeMedalFilled", "Goblet", "GobletFull", "GobletSquare",
            "GobletSquareFull", "Box", "BoxFilled", "Briefcase", "BriefcaseFilled",
            "Suitcase", "SuitcaseFilled", "SuitcaseLine", "Calendar", "CalendarFilled",
            "Clock", "ClockFilled", "Timer", "AlarmClock", "Watch",
            "Magnet", "MagnetFilled", "Cpu", "CpuFilled", "Connection",
            "ConnectionFilled", "Link", "LinkFilled", "Share", "ShareFilled",
            "Coin", "CoinFilled", "Wallet", "WalletFilled", "Money",
            "MoneyFilled", "Goods", "GoodsFilled", "ShoppingCart", "ShoppingCartFull",
            "ShoppingCartFilled", "ShoppingBag", "ShoppingBagFilled", "Present",
            "PresentFilled", "Stamp", "StampFilled", "Discount", "DiscountFilled",
            "PriceTag", "PriceTagFilled", "Ticket", "TicketFilled", "Sell",
            "SellFilled", "Sold", "SoldFilled", "Female", "Male",
            "AvatarFilled", "UserFilled3", "UserFilled4", "Coordinate",
            "CoordinateFilled", "LocationInformation", "LocationInformationFilled",
            "House", "HouseFilled", "OfficeBuilding", "OfficeBuildingFilled",
            "School", "SchoolFilled", "Bicycle", "BicycleFilled", "Van",
            "VanFilled", "Truck", "TruckFilled", "Ship", "ShipFilled",
            "Plane", "PlaneFilled", "Train", "TrainFilled", "Car",
            "CarFilled", "Bus", "BusFilled", "Help", "HelpFilled",
            "Question", "QuestionFilled2", "Eleme", "ElemeFilled", "Food",
            "FoodFilled", "Burger", "BurgerFilled", "Dish", "DishFilled",
            "Apple", "AppleFilled", "Pear", "PearFilled", "Cherry",
            "CherryFilled", "Orange", "OrangeFilled", "Coffee", "CoffeeFilled",
            "GobletFull2", "GobletSquare2", "GobletSquareFull2", "GobletFullFilled",
            "GobletSquareFilled", "IceCream", "IceCreamRound", "IceCreamSquare",
            "IceCreamFilled", "IceCreamRoundFilled", "IceCreamSquareFilled",
            "Lollipop", "LollipopFilled", "AppleFilled2", "PearFilled2",
            "CherryFilled2", "OrangeFilled2", "Mug", "MugFilled",
            "MugSaucer", "MugSaucerFilled", "WaterCup", "WaterCupFilled",
            "HotWater", "HotWaterFilled", "IceDrink", "IceDrinkFilled",
            "IceTea", "IceTeaFilled", "CoffeeCup", "CoffeeCupFilled",
            "Sugar", "SugarFilled", "Bowl", "BowlFilled", "Dessert",
            "DessertFilled", "Fork", "ForkFilled", "Knife", "KnifeFilled",
            "Spoon", "SpoonFilled", "Chicken", "ChickenFilled", "FoodFilled2",
            "DishDot", "DishDotFilled", "FoodFilled3", "BurgerFilled2",
            "AppleFilled3", "PearFilled3", "CherryFilled3", "OrangeFilled3",
            "BicycleFilled2", "VanFilled2", "TruckFilled2", "ShipFilled2",
            "PlaneFilled2", "TrainFilled2", "CarFilled2", "BusFilled2"
    ));

    public static class ValidationItem {

        private final String level;

        private final String message;

        public ValidationItem(String level, String message) {
            this.level = level;
            this.message = message;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ValidationResult {

        private final List<ValidationItem> items = new java.util.ArrayList<>();

        private boolean hasErrors;

        private boolean hasWarnings;

        public void addItem(String level, String message) {
            this.items.add(new ValidationItem(level, message));
            if ("ERROR".equals(level)) {
                this.hasErrors = true;
            } else if ("WARNING".equals(level)) {
                this.hasWarnings = true;
            }
        }

        public List<ValidationItem> getItems() {
            return items;
        }

        public boolean isHasErrors() {
            return hasErrors;
        }

        public boolean isHasWarnings() {
            return hasWarnings;
        }
    }

    public ValidationResult validateManifest(String manifestJson, List<String> zipEntryNames) {
        ValidationResult result = new ValidationResult();

        if (manifestJson == null || manifestJson.isBlank()) {
            result.addItem("ERROR", "manifest.json 内容为空");
            return result;
        }

        JsonNode manifest;
        try {
            manifest = objectMapper.readTree(manifestJson);
        } catch (Exception e) {
            result.addItem("ERROR", "manifest.json 解析失败: " + e.getMessage());
            return result;
        }

        // id
        JsonNode idNode = manifest.get("id");
        if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
            result.addItem("ERROR", "manifest 缺少 id 字段");
        } else {
            String id = idNode.asText();
            if (id.length() < 2 || id.length() > 50) {
                result.addItem("ERROR", "id 长度必须在 2-50 之间");
            } else if (!PLUGIN_ID_PATTERN.matcher(id).matches()) {
                result.addItem("ERROR", "id 必须匹配正则 ^[a-z][a-z0-9-]*$");
            }
        }

        // name
        JsonNode nameNode = manifest.get("name");
        if (nameNode == null || nameNode.isNull() || nameNode.asText().isBlank()) {
            result.addItem("ERROR", "manifest 缺少 name 字段");
        }

        // entryFile
        JsonNode entryNode = manifest.get("entryFile");
        if (entryNode == null || entryNode.isNull() || entryNode.asText().isBlank()) {
            result.addItem("ERROR", "manifest 缺少 entryFile 字段");
        } else {
            String entryFile = entryNode.asText();
            if (!entryFile.endsWith(".html")) {
                result.addItem("ERROR", "entryFile 必须以 .html 结尾");
            } else {
                boolean exists = zipEntryNames != null && zipEntryNames.stream()
                        .anyMatch(n -> n.equals(entryFile) || n.endsWith("/" + entryFile));
                if (!exists) {
                    result.addItem("ERROR", "entryFile 在 ZIP 中不存在: " + entryFile);
                }
            }
        }

        // zip entry path traversal
        if (zipEntryNames != null) {
            for (String name : zipEntryNames) {
                if (name.contains("..")) {
                    result.addItem("ERROR", "ZIP 条目包含路径穿越字符 '..': " + name);
                    break;
                }
            }

            // zip entry count
            if (zipEntryNames.size() > 100) {
                result.addItem("ERROR", "ZIP 条目数量超过 100");
            }
        }

        // version (warning)
        JsonNode versionNode = manifest.get("version");
        if (versionNode == null || versionNode.isNull() || versionNode.asText().isBlank()) {
            result.addItem("WARNING", "manifest 缺少 version 字段");
        } else if (!SEMVER_PATTERN.matcher(versionNode.asText()).matches()) {
            result.addItem("WARNING", "version 不符合 semver 规范: " + versionNode.asText());
        }

        // description (warning)
        JsonNode descNode = manifest.get("description");
        if (descNode == null || descNode.isNull() || descNode.asText().isBlank()) {
            result.addItem("WARNING", "manifest 缺少 description 字段");
        }

        // icon (warning)
        JsonNode iconNode = manifest.get("icon");
        if (iconNode == null || iconNode.isNull() || iconNode.asText().isBlank()) {
            result.addItem("WARNING", "manifest 缺少 icon 字段");
        }

        if (!result.isHasErrors()) {
            result.addItem("SUCCESS", "校验通过");
        }

        return result;
    }

    public boolean isValidIcon(String icon) {
        if (icon == null || icon.isEmpty()) {
            return true;
        }
        return VALID_ICON_NAMES.contains(icon);
    }
}
