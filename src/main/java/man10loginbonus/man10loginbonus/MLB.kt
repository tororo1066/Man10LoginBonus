package man10loginbonus.man10loginbonus

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
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

class MLB : JavaPlugin(), Listener {

    private val prefix = "[§dM§aL§eB]§b "

    override fun onEnable() {
        server.logger.info("Man10LoginBonus is Enable!")
        server.pluginManager.registerEvents(this,this)
        getCommand("mlb")?.setExecutor(this)
        saveDefaultConfig()
        if (!config.isSet("data"))return
        val next = (ZonedDateTime.now().hour * 3600 + ZonedDateTime.now().minute * 60 + ZonedDateTime.now().second)
        val nexttime = (86400 - next) * 20
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            val monthed = ZonedDateTime.now().minusHours(1).monthValue
            val month = ZonedDateTime.now().monthValue
            if (monthed != month){
                val l = config.getStringList("data")
                for (i in l){
                    l.remove(i)
                    config.set("data",l)
                }
            }
            val l = config.getStringList("data")
            for (i in l){
                val d = i.split(":")[0] + ":" + (i.split(":")[1].toInt() + 1) + ":true"
                l.remove(i)
                l.add(d)
                config.set("data",l)
            }
            saveConfig()
            server.logger.info("ログインボーナスの更新が完了しました")
            Bukkit.broadcastMessage(prefix + "ログインボーナスが更新されました！")
            Bukkit.broadcastMessage(prefix + "入りなおすと取得することができます")
        },nexttime.toLong(),86400 * 20)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player)return true

        when(args[0]){
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
                    inv.setItem(j,itemFromBase64(l[c]))
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
        val l = config.getStringList("data")
        if (l.find { it.contains("${e.player.uniqueId}") } == null) {
            l.add("${e.player.uniqueId}:0:true")
            config.set("data", l)
            saveConfig()
        }
        val find = l.find { it.contains("${e.player.uniqueId}") }!!
        val int = find.split(":")[1].toInt()
        if (!find.split(":")[2].toBoolean())return
        if (int == 25)return
        val month = ZonedDateTime.now().monthValue
        val file = File(dataFolder,"$month.yml")
        if (!file.exists()){
            e.player.sendMessage(prefix + "LoginBonusファイルを取得できませんでした")
            e.player.sendMessage(prefix + "運営にお問い合わせください")
            return
        }
        val con = YamlConfiguration.loadConfiguration(file)
        val list = con.getStringList("login")
        e.player.sendMessage("${e.player.inventory.contents.size}")
        if (e.player.inventory.firstEmpty() == -1){
            e.player.sendMessage(prefix + "インベントリの空きがありません！")
            e.player.sendMessage(prefix + "インベントリを空けてから入りなおしてください")
            return
        }
        var c = 0
        while (c != e.player.inventory.size){
            if (e.player.inventory.getItem(c)?.type == Material.AIR || e.player.inventory.getItem(c) == null){
                e.player.inventory.setItem(c,itemFromBase64(list[find.split(":")[1].toInt()]))
                break
            }
            c++
        }
        l.remove(find)
        l.add("${e.player.uniqueId}:$int:false")
        config.set("data",l)
        saveConfig()
        e.player.sendMessage(prefix + "今日のログインボーナス${itemFromBase64(list[find.split(":")[1].toInt()])?.itemMeta?.displayName}を受け取りました！")
        return
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