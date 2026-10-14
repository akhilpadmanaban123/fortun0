package app.paisapilot.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TempleHindu
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import app.paisapilot.core.model.CategoryInfo

/** An icon the user can give a category of their own. [name] is read out by screen readers. */
data class CategoryIcon(val key: String, val name: String, val image: ImageVector)

/**
 * The icon picker. Custom categories are saved with a [CategoryIcon.key], so keys must never
 * be renamed or removed; new icons can be added anywhere in the list.
 */
val CategoryIcons: List<CategoryIcon> = listOf(
    CategoryIcon("label", "Label", Icons.AutoMirrored.Rounded.Label),
    CategoryIcon("coffee", "Tea and coffee", Icons.Rounded.Coffee),
    CategoryIcon("fastfood", "Snacks", Icons.Rounded.Fastfood),
    CategoryIcon("icecream", "Sweets", Icons.Rounded.Icecream),
    CategoryIcon("bar", "Drinks", Icons.Rounded.LocalBar),
    CategoryIcon("cake", "Birthday", Icons.Rounded.Cake),
    CategoryIcon("celebration", "Party", Icons.Rounded.Celebration),
    CategoryIcon("gift", "Gift", Icons.Rounded.CardGiftcard),
    CategoryIcon("temple", "Temple", Icons.Rounded.TempleHindu),
    CategoryIcon("donation", "Donation", Icons.Rounded.VolunteerActivism),
    CategoryIcon("pets", "Pets", Icons.Rounded.Pets),
    CategoryIcon("child", "Children", Icons.Rounded.ChildCare),
    CategoryIcon("love", "Loved ones", Icons.Rounded.Favorite),
    CategoryIcon("gym", "Gym", Icons.Rounded.FitnessCenter),
    CategoryIcon("cricket", "Sports", Icons.Rounded.SportsCricket),
    CategoryIcon("games", "Games", Icons.Rounded.SportsEsports),
    CategoryIcon("music", "Music", Icons.Rounded.MusicNote),
    CategoryIcon("books", "Books", Icons.AutoMirrored.Rounded.MenuBook),
    CategoryIcon("camera", "Photos", Icons.Rounded.CameraAlt),
    CategoryIcon("art", "Hobby", Icons.Rounded.Brush),
    CategoryIcon("clothes", "Clothes", Icons.Rounded.Checkroom),
    CategoryIcon("jewellery", "Jewellery", Icons.Rounded.Diamond),
    CategoryIcon("salon", "Salon", Icons.Rounded.ContentCut),
    CategoryIcon("spa", "Self care", Icons.Rounded.Spa),
    CategoryIcon("pharmacy", "Medicines", Icons.Rounded.LocalPharmacy),
    CategoryIcon("laundry", "Laundry", Icons.Rounded.LocalLaundryService),
    CategoryIcon("repair", "Repairs", Icons.Rounded.Build),
    CategoryIcon("shop", "Local shop", Icons.Rounded.Storefront),
    CategoryIcon("flowers", "Flowers", Icons.Rounded.LocalFlorist),
    CategoryIcon("phone", "Phone", Icons.Rounded.PhoneAndroid),
    CategoryIcon("taxi", "Cab and auto", Icons.Rounded.LocalTaxi),
    CategoryIcon("train", "Train and metro", Icons.Rounded.Train),
    CategoryIcon("bike", "Bike", Icons.Rounded.TwoWheeler),
    CategoryIcon("car", "Car", Icons.Rounded.DirectionsCar),
    CategoryIcon("parking", "Parking", Icons.Rounded.LocalParking),
    CategoryIcon("savings", "Savings", Icons.Rounded.Savings),
)

val CategoryInfo.icon: ImageVector
    get() = builtIn?.icon
        ?: CategoryIcons.firstOrNull { it.key == iconKey }?.image
        ?: CategoryIcons.first().image
