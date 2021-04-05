package man10loginbonus.man10loginbonus

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.NumberFormatException
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MLB : JavaPlugin(), Listener {

    private val prefix = "[§dM§aL§eB§f]§b "
    lateinit var pool : ExecutorService
    var mode : Boolean = false

    override fun onEnable() {
        server.pluginManager.registerEvents(this,this)
        getCommand("mlb")?.setExecutor(this)
        saveDefaultConfig()
        val sql = MySQLManager(this,"mlbtestload")
        if (!sql.connected){
            server.logger.warning(prefix + "mysqlに接続できなかったのでpluginを停止しました")
            mode = false
            return
        }else mode = true
        sql.close()
        server.logger.info("Man10LoginBonus is Enable!")
        pool = Executors.newCachedThreadPool()
        val next = (ZonedDateTime.now().hour * 3600 + ZonedDateTime.now().minute * 60 + ZonedDateTime.now().second)
        val nexttime = (86400 - next) * 20
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            val monthed = ZonedDateTime.now().minusHours(1).monthValue
            val month = ZonedDateTime.now().monthValue
            val mysql = MySQLManager(this,"mlbdayload")
            if (monthed != month){
                mysql.execute("UPDATE man10loginbonus SET day = 0, boolean = 'true';")
            }else{
                mysql.execute("UPDATE man10loginbonus SET day = day + 1, boolean = 'true';")
            }
            mysql.close()
            server.logger.info("ログインボーナスの更新が完了しました")
            Bukkit.broadcastMessage(prefix + "ログインボーナスが更新されました！")
            Bukkit.broadcastMessage(prefix + "入りなおすと取得することができます")
            Bukkit.broadcastMessage(prefix + "ログインボーナスは/mlb showで見ることができます")
        },nexttime.toLong())

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player)return true
        if (args.isEmpty())return true
        if (!mode){
            sender.sendMessage(prefix + "は現在未稼働中です！")
            return true
        }
        when(args[0]){
            "help"->{
                sender.sendMessage("§b===============Man10LoginBonus===============")
                sender.sendMessage("§b/mlb show 今月のログインボーナスを見ます")
                if (sender.hasPermission("admin")){
                    sender.sendMessage("§b/mlb set (month) 指定した月のログインボーナスを変更します")
                }
                sender.sendMessage("§bAuthor:tororo_1066")
                sender.sendMessage("§b===============Man10LoginBonus===============")
                return true
            }
            "set"->{
                if (!sender.hasPermission("admin"))return true
                if (args.size != 2)return true
                try {
                    args[1].toInt()
                }catch (e : NumberFormatException){
                    sender.sendMessage(prefix + "文字列の数値化に失敗しました")
                    return true
                }
                if (args[1].toInt() !in 1..12){
                    sender.sendMessage(prefix + "1~12の値で指定してください！")
                    return true
                }
                val file = File(dataFolder,"${args[1].toInt()}.yml")
                if (!file.exists()){
                    file.createNewFile()
                    val inv = Bukkit.createInventory(null,54,"$prefix${args[1].toInt()}月のログインボーナス(設定画面)")
                    for (i in 0..53 step 9){
                        inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+1, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+7, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+8, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    }
                    for (i in 47..51){
                        if (i == 49) inv.setItem(i,ItemStack(Material.RED_STAINED_GLASS_PANE))else inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    }
                    sender.openInventory(inv)

                }else{
                    val inv = Bukkit.createInventory(null,54,"$prefix${args[1].toInt()}月のログインボーナス(設定画面)")
                    val con = YamlConfiguration.loadConfiguration(file)
                    val l = con.getStringList("login")
                    if (l.size != 25){
                        sender.sendMessage(prefix + "configファイルが壊れている可能性があります")
                        sender.sendMessage(prefix + "該当のconfigファイルを削除してください")
                        return true
                    }
                    for (i in 0..53 step 9){
                        inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+1, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+7, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                        inv.setItem(i+8, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    }
                    for (i in 47..51){
                        if (i == 49) inv.setItem(i,ItemStack(Material.RED_STAINED_GLASS_PANE))else inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    }
                    var c = 0
                    for (i in 0 until 45 step 9) for (j in (i + 2)..(i + 6)){
                        inv.setItem(j,itemFromBase64(l[c]))
                        c++
                    }
                    sender.openInventory(inv)


                }

            }
            "show"->{
                if (args.size != 1)return true

                val month = ZonedDateTime.now().monthValue
                val inv = Bukkit.createInventory(null,54,"${prefix}${month}月のログインボーナス")
                val file = File(dataFolder,"$month.yml")
                if (!file.exists()){
                    sender.sendMessage(prefix + "コンフィグファイルが見つかりませんでした")
                    return true
                }
                val con = YamlConfiguration.loadConfiguration(file)
                val l = con.getStringList("login")
                if (l.size != 25){
                    sender.sendMessage(prefix + "configファイルが壊れている可能性があります")
                    sender.sendMessage(prefix + "該当のconfigファイルを削除してください")
                    return true
                }
                for (i in 0..53 step 9){
                    inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    inv.setItem(i+1, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    inv.setItem(i+7, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                    inv.setItem(i+8, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                }
                for (i in 47..51){
                    inv.setItem(i, ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                }
                var c = 0
                for (i in 0 until 45 step 9) for (j in (i + 2)..(i + 6)){
                    if (config.getStringList("data").find { it.contains("${sender.uniqueId}") }?.split(":")?.get(1)?.toInt() == c){
                        val item = itemFromBase64(l[c])?.let { ItemStack(it) }
                        val meta = item?.itemMeta
                        meta?.addEnchant(Enchantment.ARROW_DAMAGE,0,false)
                        item?.itemMeta = meta
                        inv.setItem(j,item)
                    }else{
                        inv.setItem(j,itemFromBase64(l[c]))
                    }

                    c++
                }

                sender.openInventory(inv)


            }
        }
        return true
    }

    @EventHandler
    fun closeinv(e : InventoryCloseEvent){
        if (e.view.title.contains("月のログインボーナス(設定画面)")){
            val inv = e.inventory
            val month = e.view.title.replace(prefix,"").replace("月のログインボーナス(設定画面)","").toInt()
            val file = File(dataFolder,"$month.yml")
            val l = YamlConfiguration.loadConfiguration(file)

            val list = arrayListOf<String>()
            for (i in 0 until 45 step 9) for (j in (i + 2)..(i + 6)) {//42
                if (inv.getItem(j) == null || inv.getItem(j)?.type == Material.AIR)list.add(itemToBase64(ItemStack(Material.AIR)))else inv.getItem(j)?.let { itemToBase64(it) }?.let { list.add(it) }
            }


            l.set("login",list)
            l.save(file)
            e.player.sendMessage(prefix + "configを保存しました")
        }
    }
    @EventHandler
    fun click(e : InventoryClickEvent){
        val click = e.whoClicked as Player
        val s = e.slot
        if (e.view.title.contains("月のログインボーナス")){
            if (e.clickedInventory == click.inventory)return
            if (e.view.title.contains("月のログインボーナス(設定画面)")){
                if (s in 0..1 || s in 9..10 || s in 18..19 || s in 27..28 || s in 36..37 || s in 45..48 || s in 50..53 || s in 7..8 || s in 16..17 || s in 25..26 || s in 34..35 || s in 43..44)e.isCancelled = true
                if (e.slot == 49)click.closeInventory()
            }else e.isCancelled = true
        }


    }

    @EventHandler
    fun join(e : PlayerJoinEvent){
        if (!mode)return
        pool.execute {
            val mysql = MySQLManager(this,"mlbjoin")
            var rs = mysql.query("SELECT * FROM man10loginbonus WHERE UUID = '${e.player.uniqueId}';")
            if (!rs?.next()!!){
                mysql.execute("INSERT INTO man10loginbonus (UUID, day, boolean) VALUES ('${e.player.uniqueId}', 0, 'true');")
                rs = mysql.query("SELECT * FROM man10loginbonus WHERE UUID = '${e.player.uniqueId}';")
            }
            if (!rs?.getBoolean("boolean")!!)return@execute
            val day = rs?.getInt("day")!!
            if (day > 24)return@execute
            rs.close()

            val month = ZonedDateTime.now().monthValue
            val file = File(dataFolder,"$month.yml")
            if (!file.exists()){
                e.player.sendMessage(prefix + "LoginBonusファイルを取得できませんでした")
                e.player.sendMessage(prefix + "運営にお問い合わせください")
                return@execute
            }
            val con = YamlConfiguration.loadConfiguration(file)
            val list = con.getStringList("login")
            if (e.player.inventory.firstEmpty() == -1){
                e.player.sendMessage(prefix + "インベントリの空きがありません！")
                e.player.sendMessage(prefix + "インベントリを空けてから入りなおしてください")
                return@execute
            }
            var c = 0
            while (c != e.player.inventory.size){
                if (e.player.inventory.getItem(c)?.type == Material.AIR || e.player.inventory.getItem(c) == null){
                    mysql.execute("UPDATE man10loginbonus SET boolean = 'false' WHERE UUID = '${e.player.uniqueId}';")
                    e.player.inventory.setItem(c,itemFromBase64(list[day]))
                    e.player.sendMessage(prefix + "今日のログインボーナス${itemFromBase64(list[day])?.itemMeta?.displayName}を受け取りました！")
                    break
                }
                c++
            }
            mysql.close()
            return@execute
        }
    }


    ///////////////////////////////
    //base 64
    //////////////////////////////
    private fun itemFromBase64(data: String): ItemStack? = try {
        val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
        val dataInput = BukkitObjectInputStream(inputStream)
        val items = arrayOfNulls<ItemStack>(dataInput.readInt())

        // Read the serialized inventory
        for (i in items.indices) {
            items[i] = dataInput.readObject() as ItemStack
        }

        dataInput.close()
        items[0]
    } catch (e: Exception) {
        null
    }

    @Throws(IllegalStateException::class)
    fun itemToBase64(item: ItemStack): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)
            val items = arrayOfNulls<ItemStack>(1)
            items[0] = item
            dataOutput.writeInt(items.size)

            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            dataOutput.close()

            return Base64Coder.encodeLines(outputStream.toByteArray())

        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }


}