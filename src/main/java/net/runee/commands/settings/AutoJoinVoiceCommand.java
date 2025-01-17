package net.runee.commands.settings;

import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.runee.errors.CommandException;
import net.runee.misc.Utils;
import net.runee.misc.discord.Command;
import net.runee.model.GuildConfig;

import java.util.Locale;

public class AutoJoinVoiceCommand extends Command {
    public AutoJoinVoiceCommand() {
        super(Commands.slash("autojoin", "Mark a voice channel to be connected to after login"));
        ((SlashCommandData)data).addOption(OptionType.STRING, "op", "Operation to perform (set|clear|show)", true);
        ((SlashCommandData)data).addOption(OptionType.CHANNEL, "channel", "Voice channel in question. Only valid if op = 'set'", false);
        _public = true;
    }

    @Override
    public void run(SlashCommandInteractionEvent ctx) throws CommandException {
        ensureAdminOrOwnerPermission(ctx);

        String op = ensureOptionPresent(ctx, "op").getAsString().toLowerCase(Locale.ROOT);

        VoiceChannel channel = null;

        switch (op) {
            case "set": {
                GuildChannel guildChannel = ensureOptionPresent(ctx, "channel").getAsChannel();
                if (guildChannel instanceof VoiceChannel) {
                    channel = (VoiceChannel) guildChannel;
                } else {
                    reply(ctx, "Channel is not a voice channel", Utils.colorRed);
                    return;
                }
                break;
            }
            case "clear":
            case "show":
                ensureOptionAbsent(ctx, "channel");
                break;
            default:
                reply(ctx, "Unrecognized operation: `" + op + "`.", Utils.colorRed);
                return;
        }

        // execute
        GuildConfig guildConfig = getConfig().getGuildConfig(ctx.getGuild());

        switch (op) {
            case "set":
            case "clear":
                setAutoVoiceChannel(ctx, guildConfig, channel);
                break;
            case "show":
                showAutoVoiceChannel(ctx, guildConfig);
                break;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    private void setAutoVoiceChannel(SlashCommandInteractionEvent ctx, GuildConfig guildConfig, VoiceChannel channel) {
        guildConfig.autoJoinVoiceChannelId = channel != null ? channel.getId() : null;
        saveConfig();
        if (channel != null) {
            reply(ctx, "Auto-join voice channel updated.", Utils.colorGreen);
        } else {
            reply(ctx, "Auto-join voice channel removed.", Utils.colorGreen);
        }
    }

    private void showAutoVoiceChannel(SlashCommandInteractionEvent ctx, GuildConfig guildConfig) {
        if (guildConfig.autoJoinVoiceChannelId != null) {
            reply(ctx, "Current auto-join voice channel: " + formatChannel(ctx, guildConfig.autoJoinVoiceChannelId) + ".", Utils.colorGreen);
        } else {
            reply(ctx, "No auto-join voice channel is currently set.", Utils.colorGreen);
        }
    }

    private String formatChannel(SlashCommandInteractionEvent ctx, String channelId) {
        VoiceChannel voiceChannel = ctx.getJDA().getVoiceChannelById(channelId);
        return "`" + (voiceChannel != null ? Utils.formatChannel(voiceChannel) : channelId) + "`";
    }
}
