package com.muoffline.mobile.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.muoffline.mobile.model.*
import kotlin.math.*

@Composable
fun GameWorldCanvas(
    hero: HeroSave,
    heroFacingAngle: Float,
    monsters: List<LiveMonster>,
    drops: List<GroundDrop>,
    floatingTexts: List<FloatingText>,
    vfxList: List<SkillVfx>,
    lanPlayers: List<LanPlayer>,
    lanBosses: List<LanBossState>,
    onTapNpc: (LorenciaNpcDef) -> Unit,
    onTapWorld: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestHero by rememberUpdatedState(hero)
    val latestOnTapNpc by rememberUpdatedState(onTapNpc)
    val latestOnTapWorld by rememberUpdatedState(onTapWorld)

    var frameClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                frameClockMs = System.currentTimeMillis()
            }
        }
    }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val subLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val floatPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val currentHero = latestHero
                    val wx = currentHero.x + (offset.x - size.width / 2f)
                    val wy = currentHero.y + (offset.y - size.height / 2f)
                    if (currentHero.mapId == MapId.LORENCIA) {
                        val npc = MuCatalog.LORENCIA_NPCS.firstOrNull {
                            hypot((it.wx - wx).toDouble(), (it.wy - wy).toDouble()) <= 36.0
                        }
                        if (npc != null) {
                            latestOnTapNpc(npc)
                            return@detectTapGestures
                        }
                    }
                    latestOnTapWorld(wx, wy)
                }
            }
    ) {
        val now = frameClockMs
        val mapMeta = MuCatalog.MAP_CATALOG.getValue(hero.mapId)
        val camX = hero.x - size.width / 2f
        val camY = hero.y - size.height / 2f

        // 1. Background & Tiled Terrain
        drawRect(color = Color(mapMeta.groundPrimary))

        val tileSize = 64f
        val startTileX = floor(camX / tileSize).toInt() - 1
        val endTileX = ceil((camX + size.width) / tileSize).toInt() + 1
        val startTileY = floor(camY / tileSize).toInt() - 1
        val endTileY = ceil((camY + size.height) / tileSize).toInt() + 1

        val primaryColor = Color(mapMeta.groundPrimary)
        val secondaryColor = Color(mapMeta.groundSecondary)
        val tileGapColor = Color.Black.copy(alpha = 0.22f)

        for (tx in startTileX..endTileX) {
            for (ty in startTileY..endTileY) {
                val wx = tx * tileSize
                val wy = ty * tileSize
                val sx = wx - camX
                val sy = wy - camY
                val inWorld = wx >= 0f && wy >= 0f && wx < MuWorldConstants.WORLD_SIZE && wy < MuWorldConstants.WORLD_SIZE
                if (!inWorld) {
                    drawRect(
                        color = Color(0xFF05070B),
                        topLeft = Offset(sx, sy),
                        size = Size(tileSize + 1f, tileSize + 1f)
                    )
                    continue
                }
                val checker = ((tx + ty) and 1) == 0
                drawRect(
                    color = if (checker) primaryColor else secondaryColor,
                    topLeft = Offset(sx, sy),
                    size = Size(tileSize + 0.5f, tileSize + 0.5f)
                )
                drawRect(
                    color = tileGapColor,
                    topLeft = Offset(sx, sy),
                    size = Size(tileSize, tileSize),
                    style = Stroke(width = 1f)
                )
            }
        }

        // World boundary wall glow
        drawRect(
            color = Color(mapMeta.accentColor).copy(alpha = 0.5f),
            topLeft = Offset(-camX, -camY),
            size = Size(MuWorldConstants.WORLD_SIZE, MuWorldConstants.WORLD_SIZE),
            style = Stroke(width = 4f)
        )

        // 2. Lorencia Safe Citadel & Fountain
        val safeSx = MuWorldConstants.SAFE_CENTER - camX
        val safeSy = MuWorldConstants.SAFE_CENTER - camY
        drawCircle(
            color = Color(mapMeta.safeZoneColor).copy(alpha = 0.6f),
            radius = MuWorldConstants.SAFE_RADIUS,
            center = Offset(safeSx, safeSy)
        )
        drawCircle(
            color = Color(mapMeta.accentColor).copy(alpha = 0.7f),
            radius = MuWorldConstants.SAFE_RADIUS,
            center = Offset(safeSx, safeSy),
            style = Stroke(width = 3f)
        )

        if (hero.mapId == MapId.LORENCIA) {
            // Central Fountain
            drawCircle(
                color = Color(0xFF475569),
                radius = 34f,
                center = Offset(safeSx, safeSy)
            )
            val ripple = 14f + sin(now / 320.0).toFloat() * 4f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    center = Offset(safeSx, safeSy),
                    radius = 24f
                ),
                radius = 24f,
                center = Offset(safeSx, safeSy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = ripple,
                center = Offset(safeSx, safeSy),
                style = Stroke(width = 2f)
            )

            // 4 Torch Braziers
            val torchOffsets = listOf(
                Offset(-84f, -84f), Offset(84f, -84f),
                Offset(-84f, 84f), Offset(84f, 84f)
            )
            torchOffsets.forEachIndexed { idx, off ->
                val tx = safeSx + off.x
                val ty = safeSy + off.y
                val flicker = 1f + sin(now / 110.0 + idx).toFloat() * 0.16f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFF59E0B).copy(alpha = 0.36f), Color.Transparent),
                        center = Offset(tx, ty),
                        radius = 44f * flicker
                    ),
                    radius = 44f * flicker,
                    center = Offset(tx, ty)
                )
                drawCircle(color = Color(0xFF334155), radius = 9f, center = Offset(tx, ty))
                drawCircle(color = Color(0xFFFBBF24), radius = 5.5f * flicker, center = Offset(tx, ty))
            }

            // 4 Lorencia NPCs
            for (npc in MuCatalog.LORENCIA_NPCS) {
                val nx = npc.wx - camX
                val ny = npc.wy - camY
                if (nx < -80f || nx > size.width + 80f || ny < -80f || ny > size.height + 80f) continue
                val npcColor = when (npc.id) {
                    "smith" -> Color(0xFFF97316)
                    "wizard" -> Color(0xFF38BDF8)
                    "barmaid" -> Color(0xFFEC4899)
                    else -> Color(0xFFFBBF24)
                }
                drawCircle(color = npcColor.copy(alpha = 0.24f), radius = 24f, center = Offset(nx, ny))
                drawCircle(color = npcColor, radius = 14f, center = Offset(nx, ny))
                drawCircle(color = Color(0xFFFDE68A), radius = 7f, center = Offset(nx, ny - 13f))
                drawIntoCanvas { canvas ->
                    labelPaint.color = Color(0xFFFDE68A).toArgb()
                    canvas.nativeCanvas.drawText(npc.name, nx, ny - 28f, labelPaint)
                    subLabelPaint.color = Color(0xFFCBD5E1).toArgb()
                    canvas.nativeCanvas.drawText(npc.subtitle, nx, ny + 32f, subLabelPaint)
                }
            }
        }

        // 3. Ground Drops
        for (drop in drops) {
            val dx = drop.x - camX
            val dy = drop.y - camY
            if (dx < -40f || dx > size.width + 40f || dy < -40f || dy > size.height + 40f) continue
            val pulse = 1f + sin(now / 180.0).toFloat() * 0.14f
            if (drop.zen != null) {
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.28f),
                    radius = 15f * pulse,
                    center = Offset(dx, dy)
                )
                drawCircle(color = Color(0xFFF59E0B), radius = 7f, center = Offset(dx, dy))
                drawIntoCanvas { canvas ->
                    subLabelPaint.color = Color(0xFFFDE68A).toArgb()
                    canvas.nativeCanvas.drawText("${drop.zen} Zen", dx, dy - 16f, subLabelPaint)
                }
            } else if (drop.item != null) {
                val c = Color(drop.item.colorHex)
                drawCircle(color = c.copy(alpha = 0.30f), radius = 17f * pulse, center = Offset(dx, dy))
                drawRoundRect(
                    color = c,
                    topLeft = Offset(dx - 7f, dy - 7f),
                    size = Size(14f, 14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawIntoCanvas { canvas ->
                    subLabelPaint.color = c.toArgb()
                    canvas.nativeCanvas.drawText(drop.item.name, dx, dy - 18f, subLabelPaint)
                }
            }
        }

        // 4. Monsters
        for (m in monsters) {
            if (m.hp <= 0) continue
            val mx = m.x - camX
            val my = m.y - camY
            if (mx < -80f || mx > size.width + 80f || my < -80f || my > size.height + 80f) continue
            val flashing = now < m.hitFlashUntil
            val bodyColor = if (flashing) Color.White else Color(m.colorHex)

            if (m.isBoss) {
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.28f),
                    radius = m.radius + 12f,
                    center = Offset(mx, my),
                    style = Stroke(width = 3f)
                )
            }
            // Ground shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = Offset(mx - m.radius * 0.85f, my + m.radius * 0.45f),
                size = Size(m.radius * 1.7f, m.radius * 0.65f)
            )
            // Monster body
            drawCircle(color = bodyColor, radius = m.radius, center = Offset(mx, my))
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = m.radius,
                center = Offset(mx, my),
                style = Stroke(width = 2.5f)
            )
            // Eyes
            drawCircle(color = Color(0xFFFEF08A), radius = 3.2f, center = Offset(mx - m.radius * 0.32f, my - m.radius * 0.15f))
            drawCircle(color = Color(0xFFFEF08A), radius = 3.2f, center = Offset(mx + m.radius * 0.32f, my - m.radius * 0.15f))

            // Overhead HP Bar & Name
            val barW = max(44f, m.radius * 2.4f)
            val barTop = my - m.radius - 14f
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(mx - barW / 2f, barTop),
                size = Size(barW, 6f)
            )
            val hpRatio = (m.hp.toFloat() / max(1, m.maxHp)).coerceIn(0f, 1f)
            drawRect(
                color = if (m.isBoss) Color(0xFFF59E0B) else Color(0xFFEF4444),
                topLeft = Offset(mx - barW / 2f, barTop),
                size = Size(barW * hpRatio, 6f)
            )
            drawIntoCanvas { canvas ->
                subLabelPaint.color = if (m.isBoss) Color(0xFFFBBF24).toArgb() else Color.White.toArgb()
                canvas.nativeCanvas.drawText("${m.name} Nv.${m.level}", mx, barTop - 5f, subLabelPaint)
            }
        }

        // 5. Cooperative LAN Bosses on current map
        for (lb in lanBosses) {
            if (lb.mapId != hero.mapId || lb.hp <= 0) continue
            val bx = lb.x - camX
            val by = lb.y - camY
            val pulse = 36f + sin(now / 150.0).toFloat() * 4f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.32f),
                radius = pulse + 10f,
                center = Offset(bx, by),
                style = Stroke(width = 4f)
            )
            drawCircle(color = Color(0xFF0284C7), radius = 34f, center = Offset(bx, by))
            drawCircle(color = Color(0xFFFBBF24), radius = 34f, center = Offset(bx, by), style = Stroke(width = 3f))
            val ratio = (lb.hp.toFloat() / max(1, lb.maxHp)).coerceIn(0f, 1f)
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(bx - 42f, by - 48f), size = Size(84f, 8f))
            drawRect(color = Color(0xFF38BDF8), topLeft = Offset(bx - 42f, by - 48f), size = Size(84f * ratio, 8f))
            drawIntoCanvas { canvas ->
                labelPaint.color = Color(0xFF38BDF8).toArgb()
                canvas.nativeCanvas.drawText("[BOSS LAN] ${lb.name}", bx, by - 55f, labelPaint)
            }
        }

        // 6. Remote LAN Players on same map
        for (lp in lanPlayers) {
            if (lp.id == hero.id || lp.mapId != hero.mapId) continue
            val px = lp.x - camX
            val py = lp.y - camY
            drawHeroAvatar(
                x = px,
                y = py,
                classId = lp.classId,
                facingAngle = lp.dir,
                wingsTier = lp.wingsTier,
                weaponPlus = lp.weaponPlus,
                hp = lp.hp,
                maxHp = lp.maxHp,
                displayName = "${lp.name} [Nv.${lp.level}]",
                nowMs = now,
                isLocal = false,
                labelPaint = labelPaint
            )
        }

        // 7. Local Hero Avatar
        val heroWingsTier = when {
            hero.equipment.wings == null -> 0
            hero.equipment.wings.rarity == ItemRarity.MYTHIC -> 2
            else -> 1
        }
        drawHeroAvatar(
            x = size.width / 2f,
            y = size.height / 2f,
            classId = hero.classId,
            facingAngle = heroFacingAngle,
            wingsTier = heroWingsTier,
            weaponPlus = hero.equipment.weapon?.plus ?: 0,
            hp = hero.hp,
            maxHp = hero.maxHp,
            displayName = "${hero.name} [Nv.${hero.level}]",
            nowMs = now,
            isLocal = true,
            labelPaint = labelPaint
        )

        // 8. Skill VFX
        for (vfx in vfxList) {
            val age = (now - vfx.createdAt).coerceAtLeast(0L)
            val progress = (age.toFloat() / max(1L, vfx.durationMs)).coerceIn(0f, 1f)
            val alpha = (1f - progress) * 0.85f
            val sx = vfx.x - camX
            val sy = vfx.y - camY
            val tx = vfx.targetX - camX
            val ty = vfx.targetY - camY
            val c = Color(vfx.colorHex).copy(alpha = alpha)

            when (vfx.skillId) {
                "evil-spirit", "twisting-slash", "rageful-blow", "ice-storm" -> {
                    val curRadius = vfx.radius * (0.35f + progress * 0.75f)
                    drawCircle(color = c, radius = curRadius, center = Offset(sx, sy), style = Stroke(width = 6f))
                    drawCircle(color = c.copy(alpha = alpha * 0.25f), radius = curRadius * 0.85f, center = Offset(sx, sy))
                    for (orb in 0..3) {
                        val ang = progress * 6.28f + orb * 1.57f
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = 5f,
                            center = Offset(sx + cos(ang) * curRadius * 0.7f, sy + sin(ang) * curRadius * 0.7f)
                        )
                    }
                }
                "heal-aura" -> {
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = alpha * 0.45f),
                        radius = vfx.radius * (0.4f + progress * 0.6f),
                        center = Offset(sx, sy)
                    )
                }
                else -> {
                    drawLine(
                        color = c,
                        start = Offset(sx, sy),
                        end = Offset(tx, ty),
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = c.copy(alpha = alpha * 0.45f),
                        radius = vfx.radius * progress,
                        center = Offset(tx, ty)
                    )
                }
            }
        }

        // 9. Floating Damage / Loot Text
        drawIntoCanvas { canvas ->
            for (ft in floatingTexts) {
                val age = (now - ft.createdAt).coerceAtLeast(0L)
                val p = (age / 950f).coerceIn(0f, 1f)
                val fx = ft.x - camX
                val fy = (ft.y - camY) - p * 34f
                val alphaInt = ((1f - p) * 255).toInt().coerceIn(0, 255)
                val baseColor = Color(ft.colorHex)
                floatPaint.textSize = if (ft.isCrit) 30f else 24f
                floatPaint.color = android.graphics.Color.argb(
                    alphaInt,
                    (baseColor.red * 255).toInt(),
                    (baseColor.green * 255).toInt(),
                    (baseColor.blue * 255).toInt()
                )
                canvas.nativeCanvas.drawText(ft.text, fx, fy, floatPaint)
            }
        }
    }
}

private fun DrawScope.drawHeroAvatar(
    x: Float,
    y: Float,
    classId: ClassId,
    facingAngle: Float,
    wingsTier: Int,
    weaponPlus: Int,
    hp: Int,
    maxHp: Int,
    displayName: String,
    nowMs: Long,
    isLocal: Boolean,
    labelPaint: Paint
) {
    val accent = Color(MuCatalog.CLASS_CATALOG.getValue(classId).accentColor)

    // +7..+13 spinning aura ring
    if (weaponPlus >= 7) {
        val rot = (nowMs % 1600L) / 1600f * 360f
        val auraColor = if (weaponPlus >= 11) Color(0xFF38BDF8) else Color(0xFFFBBF24)
        drawCircle(
            color = auraColor.copy(alpha = 0.45f),
            radius = 27f,
            center = Offset(x, y),
            style = Stroke(width = 3f)
        )
        val rad = Math.toRadians(rot.toDouble())
        drawCircle(
            color = auraColor,
            radius = 4f,
            center = Offset(x + (cos(rad) * 27f).toFloat(), y + (sin(rad) * 27f).toFloat())
        )
    }

    // Animated Wings if equipped
    if (wingsTier > 0) {
        val flap = sin(nowMs / 130.0).toFloat() * 6f
        val wingColor = if (wingsTier >= 2) Color(0xFFFBBF24) else Color(0xFFA855F7)
        val leftWing = Path().apply {
            moveTo(x - 6f, y)
            lineTo(x - 32f, y - 16f + flap)
            lineTo(x - 24f, y + 10f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(x + 6f, y)
            lineTo(x + 32f, y - 16f + flap)
            lineTo(x + 24f, y + 10f)
            close()
        }
        drawPath(leftWing, color = wingColor.copy(alpha = 0.72f))
        drawPath(rightWing, color = wingColor.copy(alpha = 0.72f))
    }

    // Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(x - 16f, y + 10f),
        size = Size(32f, 12f)
    )

    // Hero Body + Class Weapon Indicator
    drawCircle(color = accent, radius = 17f, center = Offset(x, y))
    drawCircle(
        color = if (isLocal) Color(0xFFFDE68A) else Color.White,
        radius = 17f,
        center = Offset(x, y),
        style = Stroke(width = 2.5f)
    )

    val wx = x + sin(facingAngle) * 22f
    val wy = y + cos(facingAngle) * 22f
    drawLine(
        color = when (classId) {
            ClassId.DK -> Color(0xFFE2E8F0)
            ClassId.DW -> Color(0xFF38BDF8)
            ClassId.ELF -> Color(0xFF34D399)
        },
        start = Offset(x, y),
        end = Offset(wx, wy),
        strokeWidth = 5f
    )

    // Overhead HP & Name
    val ratio = (hp.toFloat() / max(1, maxHp)).coerceIn(0f, 1f)
    drawRect(color = Color(0xFF0F172A), topLeft = Offset(x - 26f, y - 30f), size = Size(52f, 5f))
    drawRect(
        color = if (isLocal) Color(0xFF22C55E) else Color(0xFF38BDF8),
        topLeft = Offset(x - 26f, y - 30f),
        size = Size(52f * ratio, 5f)
    )
    drawIntoCanvas { canvas ->
        labelPaint.color = if (isLocal) Color(0xFFFDE68A).toArgb() else Color(0xFFBAE6FD).toArgb()
        canvas.nativeCanvas.drawText(displayName, x, y - 37f, labelPaint)
    }
}
